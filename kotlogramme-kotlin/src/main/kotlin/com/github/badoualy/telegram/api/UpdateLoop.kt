package com.github.badoualy.telegram.api

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The background thread behind [UpdatesApi.startUpdateLoop].
 *
 * A client owns one of these and it runs at most one thread at a time. The thread is a daemon,
 * because a loop a caller forgot to stop must not be able to hold a host application's shutdown
 * open, and its name carries [THREAD_NAME_PREFIX] so it is recognisable in a thread dump.
 *
 * The loop waits [pollTimeoutMillis] for one update rather than the 30 s
 * [UpdatesApi.getNextTypedUpdate] default. That is what keeps a stop prompt: the stop flag is only
 * read between two waits, so a stop waits for at most one poll and not for the whole timeout.
 * Nothing has to interrupt the thread to achieve that, because grammers' stream is a channel a
 * wait ends on - a short timeout costs one timed receive, not a request to Telegram.
 *
 * A callback that throws ends the loop. The update has already been read off the one shared stream,
 * so a handler that failed halfway cannot be given that update again, and running the next update
 * against whatever state it left behind is worse than stopping. The cause is kept in [failure] and
 * handed to the `onError` handler the caller supplied; with no handler it reaches the thread's
 * uncaught-exception handler, which prints it, because a background thread that dies quietly is the
 * one failure a caller would never notice.
 */
class UpdateLoop(private val updates: UpdatesApi) {
    /**
     * Starts the loop, dispatching every update to [callback] until it is stopped.
     *
     * [pollTimeoutMillis] is how long one iteration waits for an update, and therefore how long a
     * [stop] can take. [onError] is called with the cause of a failure that ends the loop; without
     * one the cause reaches the thread's uncaught-exception handler instead.
     *
     * Returns whether the thread was started. A loop that is already running is left as it is and
     * the call reports false, so a second start cannot put a second reader on the one update
     * stream.
     */
    fun start(
        callback: UpdateCallback,
        pollTimeoutMillis: Long = DEFAULT_POLL_TIMEOUT_MILLIS,
        onError: ((Throwable) -> Unit)? = null,
    ): Boolean {
        require(pollTimeoutMillis > 0) { "pollTimeoutMillis must be positive" }

        val worker = Thread(
            { run(callback, pollTimeoutMillis, onError) },
            THREAD_NAME_PREFIX + THREAD_COUNT.incrementAndGet(),
        )
        worker.isDaemon = true

        synchronized(lock) {
            if (thread?.isAlive == true) {
                return false
            }
            stopped.set(false)
            lastFailure = null
            thread = worker
            worker.start()
        }
        return true
    }

    /**
     * Stops the loop and waits for its thread.
     *
     * The wait is one [start] `pollTimeoutMillis` at most, plus whatever the callback it was
     * dispatching is doing; a caller whose own handler blocks is the only thing that can make this
     * slow. Stopping a loop that was never started, or stopping one twice, does nothing.
     */
    fun stop() {
        val worker: Thread
        synchronized(lock) {
            stopped.set(true)
            worker = thread ?: return
        }
        // A callback that stops its own loop cannot join the thread it is running on. The flag is
        // set, so the loop finishes the update it is on and exits, and the next stop from another
        // thread joins it.
        if (worker === Thread.currentThread()) {
            return
        }
        worker.join()
        synchronized(lock) {
            // Only this thread's reference, so a loop started while this one was ending survives.
            if (thread === worker) {
                thread = null
            }
        }
    }

    /** Whether the loop's thread is running. */
    val isRunning: Boolean get() = thread?.isAlive == true

    /** The throwable that ended the loop, or null while it has not failed. */
    val failure: Throwable? get() = lastFailure

    private fun run(callback: UpdateCallback, pollTimeoutMillis: Long, onError: ((Throwable) -> Unit)?) {
        try {
            while (!stopped.get()) {
                updates.dispatchNextUpdate(callback, pollTimeoutMillis)
            }
        } catch (failure: Throwable) {
            synchronized(lock) {
                lastFailure = failure
            }
            if (onError == null) {
                throw failure
            }
            onError(failure)
        }
    }

    private val lock = Any()
    private val stopped = AtomicBoolean()

    @Volatile
    private var thread: Thread? = null

    @Volatile
    private var lastFailure: Throwable? = null

    companion object {
        /**
         * The wait one iteration applies: short enough that a stop is prompt, long enough that a
         * quiet stream is not polled in a spin.
         */
        const val DEFAULT_POLL_TIMEOUT_MILLIS: Long = 250

        /** The prefix of the loop thread's name, so a caller can find it in a thread dump. */
        const val THREAD_NAME_PREFIX: String = "kotlogramme-update-loop"

        private val THREAD_COUNT = AtomicInteger()
    }
}