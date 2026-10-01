package com.github.badoualy.telegram.api

import org.kotlogramme.TelegramClient as GrammersClient
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the background loop behind the legacy [UpdateCallback].
 *
 * The loop is driven through the real [UpdatesApi] surface, but the update stream underneath is a
 * fake: [FakeStream] hands out [TypedUpdate] values from a queue and waits for the poll timeout when
 * the queue is empty, exactly as the native `nextUpdate` waits on grammers' channel. So the tests
 * exercise the lifecycle, the shutdown latency and the failure rule without a session, and the
 * "stop is prompt" case costs the one poll timeout rather than the 30 s it would cost against the
 * `dispatchNextUpdate` default.
 */
class UpdateLoopTest {
    @Test
    fun `a second start reports that the loop is already running and adds no thread`() {
        val client = FakeClient()
        val delivered = CopyOnWriteArrayList<TypedUpdate>()
        val loopsBefore = loopThreads()

        assertTrue(client.startUpdateLoop({ _, update -> delivered += update }))
        assertFalse(client.startUpdateLoop({ _, update -> delivered += update }))
        assertTrue(client.isUpdateLoopRunning())

        client.stream.offer(typed("newMessage"))
        client.stream.offer(typed("callbackQuery"))
        awaitUpTo(5_000) { delivered.size == 2 }

        // Counted while the loop is still running: stop joins its thread, so afterwards there is
        // none left to count.
        assertEquals(loopsBefore + 1, loopThreads(), "the second start made a second thread")
        client.stopUpdateLoop()

        assertEquals(1, client.pollingThreads.distinct().size, "one loop polls from one thread")
        assertFalse(client.isUpdateLoopRunning())
        assertNull(client.updateLoopFailure())
    }

    @Test
    fun `stop joins the loop thread within one poll instead of the 30 second default`() {
        val client = FakeClient()
        // A stream that never delivers anything: the thread is always inside the wait, which is the
        // case a naive stop would block on for the whole dispatchNextUpdate timeout.
        assertTrue(client.startUpdateLoop({ _, _ -> }))
        awaitUpTo(5_000) { client.polls.isNotEmpty() }
        assertEquals(
            UpdateLoop.DEFAULT_POLL_TIMEOUT_MILLIS,
            client.polls.first(),
            "the loop has to poll in the short wait, not in the default one",
        )

        val elapsed = timed { client.stopUpdateLoop() }

        assertTrue(elapsed < 5_000, "stop took ${elapsed}ms")
        assertFalse(client.isUpdateLoopRunning())
        assertNull(client.updateLoopFailure())
    }

    @Test
    fun `a throwing callback ends the loop and hands its cause to the error handler`() {
        val client = FakeClient()
        val failure = IllegalStateException("the handler failed")
        val reported = CopyOnWriteArrayList<Throwable>()
        val reportedLatch = CountDownLatch(1)
        client.stream.offer(typed("callbackQuery"))

        assertTrue(
            client.startUpdateLoop({ _, _ -> throw failure }) { thrown ->
                reported += thrown
                reportedLatch.countDown()
            },
        )

        assertTrue(reportedLatch.await(5, TimeUnit.SECONDS), "the error handler was never called")
        assertSame(failure, reported.single())
        assertSame(failure, client.updateLoopFailure())
        awaitUpTo(5_000) { !client.isUpdateLoopRunning() }
        assertFalse(client.isUpdateLoopRunning(), "a failed callback must not leave the loop running")
        client.stopUpdateLoop()
    }

    @Test
    fun `a throwing callback without an error handler still leaves its cause readable`() {
        val client = FakeClient()
        val failure = IllegalStateException("the handler failed")
        val sawTheUpdate = CountDownLatch(1)
        client.stream.offer(typed("callbackQuery"))

        // With no handler the cause goes to the thread's uncaught-exception handler, which prints
        // it in this test's output. What the test pins is that it is not lost: the loop stops and
        // the cause is still there to be read.
        assertTrue(
            client.startUpdateLoop({ _, _ ->
                sawTheUpdate.countDown()
                throw failure
            }),
        )

        assertTrue(sawTheUpdate.await(5, TimeUnit.SECONDS), "the callback was never reached")
        awaitUpTo(5_000) { client.updateLoopFailure() != null }
        assertSame(failure, client.updateLoopFailure())
        awaitUpTo(5_000) { !client.isUpdateLoopRunning() }
        assertFalse(client.isUpdateLoopRunning())
        client.stopUpdateLoop()
    }

    @Test
    fun `stopping a loop that was never started does nothing`() {
        val client = FakeClient()

        client.stopUpdateLoop()
        client.stopUpdateLoop()

        assertFalse(client.isUpdateLoopRunning())
        assertNull(client.updateLoopFailure())
        assertTrue(client.polls.isEmpty(), "a loop that was never started must not poll")
        assertEquals(0, loopThreads())
    }

    @Test
    fun `a loop can be started again after it was stopped`() {
        val client = FakeClient()
        val delivered = CopyOnWriteArrayList<String>()

        assertTrue(client.startUpdateLoop({ _, update -> delivered += update.kind }))
        client.stopUpdateLoop()
        assertTrue(client.startUpdateLoop({ _, update -> delivered += update.kind }))
        client.stream.offer(typed("newMessage"))

        awaitUpTo(5_000) { delivered.size == 1 }
        client.stopUpdateLoop()
        assertEquals(listOf("newMessage"), delivered)
    }

    @Test
    fun `a poll timeout that is not positive is rejected before any thread starts`() {
        val client = FakeClient()

        val rejected = runCatching { client.startUpdateLoop({ _, _ -> }, pollTimeoutMillis = 0) }

        assertTrue(rejected.isFailure)
        assertFalse(client.isUpdateLoopRunning())
        assertEquals(0, loopThreads())
    }

    private fun loopThreads(): Int = Thread.getAllStackTraces().keys.count {
        it.name.startsWith(UpdateLoop.THREAD_NAME_PREFIX)
    }

    private companion object {
        fun typed(kind: String) = TypedUpdate(kind = kind)

        /** Polls [condition] until it holds, or fails the test once [timeoutMillis] has passed. */
        fun awaitUpTo(timeoutMillis: Long, condition: () -> Boolean) {
            val deadline = System.nanoTime() + timeoutMillis * 1_000_000
            while (!condition() && System.nanoTime() < deadline) {
                Thread.sleep(5)
            }
            assertTrue(condition(), "the condition did not hold within ${timeoutMillis}ms")
        }

        fun timed(block: () -> Unit): Long {
            val started = System.nanoTime()
            block()
            return (System.nanoTime() - started) / 1_000_000
        }
    }
}

/**
 * The quiet-or-one-update stream a loop polls, standing in for the native `nextUpdate`.
 *
 * [polls] records the timeout of every wait, so a test can pin the wait the loop chose, and
 * [pollingThreads] the threads that made them, so a test can tell one loop from two.
 */
private class FakeStream {
    private val updates = LinkedBlockingQueue<TypedUpdate>()

    fun offer(update: TypedUpdate) {
        updates.put(update)
    }

    fun next(timeoutMillis: Long): TypedUpdate? = updates.poll(timeoutMillis, TimeUnit.MILLISECONDS)
}

/** A client whose update stream is a [FakeStream], so the loop can run without a session. */
private class FakeClient : TelegramClient {
    val stream = FakeStream()

    /** The timeouts the loop waited for, in order. */
    val polls = CopyOnWriteArrayList<Long>()

    /** The thread that each poll ran on. */
    val pollingThreads = CopyOnWriteArrayList<Long>()

    override val updateLoop = UpdateLoop(this)

    override val bridge: GrammersClient
        get() = throw UnsupportedOperationException("the fake has no grammers session")

    override fun isClosed(): Boolean = closed

    override fun close() {
        updateLoop.stop()
        closed = true
    }

    override fun authImportBotAuthorization(botAuthToken: String): Authorization =
        throw UnsupportedOperationException("the fake cannot sign in")

    override fun getNextTypedUpdate(timeoutMillis: Long): TypedUpdate? {
        polls += timeoutMillis
        pollingThreads += Thread.currentThread().id
        return stream.next(timeoutMillis)
    }

    private var closed = false
}