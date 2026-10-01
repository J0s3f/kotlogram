package com.github.badoualy.telegram.api

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Proves against a live session that a short poll does not lose updates.
 *
 * The native `nextUpdate` used to wrap `tokio::time::timeout` around grammers' own
 * `UpdateStream::next`, which cancels it. grammers takes a batch off its channel and then awaits
 * while it builds the peer map, so a cancellation in that window discards a batch that is already
 * off the channel and not yet buffered — the update is gone. At the 30 s default that window is a
 * rare coincidence; the 250 ms poll [UpdatesApi.startUpdateLoop] uses turns it into a routine one.
 *
 * The unit tests in `native/src/update_pump/tests.rs` drive a fake producer and pin the pump's
 * shape, but only a real session can show that the real stream, at the real poll timeout, delivers
 * everything. This test sends a known number of messages and then polls them back at the short
 * timeout repeatedly, so the consumer gives up many times inside the window that used to lose data.
 *
 * Its two halves are a unit test and a live test:
 *  - [the configured poll is the short one the loop uses] always runs, because it pins that the
 *    loop is not silently passing the 30 s default that would make this test vacuous.
 *  - [a short poll over a live stream delivers every update] needs a session and is gated.
 */
class LiveUpdatePollIntegrityTest {
    /**
     * The whole point of the loop's short poll is that the timeout is short; a regression that
     * passed the 30 s default would make the live test below pass without ever exercising the
     * cancellation window, so this pins the constant independently.
     */
    @Test
    fun `the configured poll is the short one the loop uses`() {
        val shortPoll = UpdateLoop.DEFAULT_POLL_TIMEOUT_MILLIS
        assertTrue(shortPoll > 0, "the poll timeout must be positive")
        assertTrue(shortPoll <= 1_000, "the loop poll must stay short, was ${shortPoll}ms")
        assertTrue(
            shortPoll < DISPATCH_DEFAULT_MILLIS,
            "the loop poll must be shorter than the dispatch default, was ${shortPoll}ms",
        )
    }

    @Test
    fun `a short poll over a live stream delivers every update`() {
        val config = LivePollConfig.loadOrSkip() ?: return
        val application = TelegramApp(config.apiId, config.apiHash)

        Kotlogram.getDefaultClient(application, FileTelegramApiStorage(config.userSession)).use { user ->
            assertTrue(user.isAuthorized(), "The configured user session is not authorized")
            val channel = user.contactsResolveUsername(config.channelUsername)
            val token = "kotlogramme-poll-${UUID.randomUUID()}"
            val sent = SENT_COUNT

            for (index in 1..sent) {
                user.messagesSendMessage(channel, "$token-$index")
            }

            // Below the 30 s default, and low enough that each poll gives up while the stream is
            // still in the window grammers takes a batch off its channel and awaits in.
            val pollMillis = UpdateLoop.DEFAULT_POLL_TIMEOUT_MILLIS
            val received = linkedSetOf<String>()
            val deadline = System.nanoTime() + LIVE_BUDGET_NANOS

            // The pump buffers what arrives between polls, so the only way to miss one is the bug
            // this test exists to catch.
            while (received.size < sent && System.nanoTime() < deadline) {
                val update = user.getNextTypedUpdate(timeoutMillis = pollMillis) ?: continue
                val text = update.message?.text ?: continue
                if (text.startsWith("$token-")) received += text
            }

            // A batch lost in the cancellation window would leave a hole here, which is exactly the
            // failure this pins: the count comes up short even though the stream is healthy.
            assertEquals(
                sent,
                received.size,
                "the short poll dropped updates; got ${received.size} of $sent after many give-ups",
            )
            val expected = (1..sent).map { "$token-$it" }.toSet()
            assertEquals(expected, received, "every sent message must have been delivered exactly once")

            // The loop drives the same stream at the same timeout, and a second start while it runs
            // must not put a second reader on it.
            assertTrue(user.startUpdateLoop({ _, _ -> }), "the loop did not start")
            assertTrue(user.isUpdateLoopRunning(), "the loop reports not running after a start")
            assertTrue(!user.startUpdateLoop({ _, _ -> }), "a second start must report false")
            user.stopUpdateLoop()
            assertTrue(!user.isUpdateLoopRunning(), "the loop reports running after a stop")
            assertNull(user.updateLoopFailure(), "a loop that was only started and stopped must not have failed")
        }
    }

    private companion object {
        /** Enough messages that the window is entered many times, not one lucky round. */
        const val SENT_COUNT = 12

        /** The live budget, after which the test reports what it actually collected. */
        const val LIVE_BUDGET_NANOS = 90_000_000_000L

        /** `UpdatesApi.dispatchNextUpdate`'s own default, which the loop's poll must undercut. */
        const val DISPATCH_DEFAULT_MILLIS = 30_000L
    }
}

/** The live configuration this test reads, kept separate from the other live suite's loader. */
private data class PollLiveConfig(
    val apiId: Int,
    val apiHash: String,
    val userSession: Path,
    val channelUsername: String,
)

private object LivePollConfig {
    fun loadOrSkip(): PollLiveConfig? {
        if (System.getenv("KOTLOGRAMME_RUN_LIVE_TESTS") != "true") {
            println("Skipping live Telegram test; set KOTLOGRAMME_RUN_LIVE_TESTS=true to enable it.")
            return null
        }
        fun required(name: String): String = requireNotNull(System.getenv(name)?.takeIf(String::isNotBlank)) {
            "$name must be set when KOTLOGRAMME_RUN_LIVE_TESTS=true"
        }
        return PollLiveConfig(
            apiId = required("KOTLOGRAMME_TEST_API_ID").toInt(),
            apiHash = required("KOTLOGRAMME_TEST_API_HASH"),
            userSession = Path.of(required("KOTLOGRAMME_TEST_USER_SESSION")),
            channelUsername = required("KOTLOGRAMME_TEST_CHANNEL_USERNAME"),
        )
    }
}
