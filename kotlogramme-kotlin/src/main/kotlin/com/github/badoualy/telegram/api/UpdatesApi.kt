package com.github.badoualy.telegram.api

/**
 * Consuming grammers' ordered update stream.
 *
 * [getNextUpdate] is the pair of fields the facade started with. [getNextTypedUpdate] is the same
 * call with the whole projection, and [getNextRawUpdate] is the same call again with the update
 * exactly as Telegram sent it; all three read one stream in turn rather than each holding their
 * own, so consuming them alternates between the typed and the untyped view of the same updates.
 *
 * [dispatchNextUpdate] waits for one of those updates by hand, and [startUpdateLoop] is the same
 * wait on a thread the caller starts and stops. The loop is the one piece of state an
 * implementation of this interface has to own, so [updateLoop] is declared rather than derived.
 */
interface UpdatesApi : BridgeApi {
    fun getNextUpdate(timeoutMillis: Long = 30_000): TelegramUpdate? =
        bridge.nextUpdate(timeoutMillis)?.toCompatibility()

    /** The next update with the whole projection, or null after [timeoutMillis]. */
    fun getNextTypedUpdate(timeoutMillis: Long = 30_000): TypedUpdate? =
        bridge.nextUpdate(timeoutMillis)?.toTypedCompatibility()

    /** The next update as Telegram sent it, or null after [timeoutMillis]. */
    fun getNextRawUpdate(timeoutMillis: Long = 30_000): RawUpdateEntry? =
        bridge.nextRawUpdate(timeoutMillis)?.toCompatibility()

    /**
     * Asks grammers to write the update state again.
     *
     * grammers writes it whenever the stream is dropped, so this is for a client that wants to
     * persist the state without closing the session.
     */
    fun syncUpdateState() = bridge.syncUpdateState()

    /**
     * Answers the guest-chat query [queryId] with the article [result].
     *
     * This is the send behind grammers' own `GuestChatQuery::answer`. The query id is read off a
     * projected `GuestChatQueryUpdate`. The result is the identifier of the inline message the
     * answer produced, which a caller can edit later.
     */
    fun answerGuestChatQuery(queryId: Long, result: InlineArticle): InlineMessageId =
        bridge.answerGuestChatQuery(queryId, result.asArticleSpec()).toCompatibility()

    /**
     * Waits for one update and hands it to [callback].
     *
     * A stream that stays quiet for [timeoutMillis] is not an error and reaches no callback, so a
     * caller driving a loop can simply call this again.
     */
    fun dispatchNextUpdate(callback: UpdateCallback, timeoutMillis: Long = 30_000) {
        // `this` is typed as the interface, but the facade client is its only implementation and
        // the callback takes the client so a handler can answer on the same session.
        val client = this as TelegramClient
        getNextTypedUpdate(timeoutMillis)?.let { callback.onUpdate(client, it) }
    }

    /**
     * Runs [callback] on a background thread for as long as the loop keeps up.
     *
     * This is [dispatchNextUpdate] called again and again, so an application that wants the Kotlogram
     * shape - a handler registered once and left to run - does not have to write the loop itself.
     * Because the one update stream is shared with a caller's own `getNextUpdate` calls, the loop
     * polls in short waits ([UpdateLoop.DEFAULT_POLL_TIMEOUT_MILLIS]) rather than in the 30 s wait
     * [dispatchNextUpdate] defaults to, which is also what keeps [stopUpdateLoop] prompt.
     *
     * Returns whether the loop was started; a second call while one is running reports false and
     * leaves the running loop alone.
     *
     * [onError] receives the cause of a failure that ends the loop, and with no handler the cause
     * reaches the loop thread's uncaught-exception handler. It must not stop the loop from inside
     * the callback: that thread cannot be joined by itself.
     */
    fun startUpdateLoop(
        callback: UpdateCallback,
        pollTimeoutMillis: Long = UpdateLoop.DEFAULT_POLL_TIMEOUT_MILLIS,
        onError: ((Throwable) -> Unit)? = null,
    ): Boolean = updateLoop.start(callback, pollTimeoutMillis, onError)

    /**
     * Stops the loop started by [startUpdateLoop] and joins its thread.
     *
     * The join waits for at most one [startUpdateLoop] poll, plus whatever the callback being
     * dispatched is doing, so it does not wait out a 30 s update timeout. Stopping a loop that was
     * never started is a no-op, and so is stopping one twice. A closed client stops its loop here
     * as well, before the session behind it goes away.
     */
    fun stopUpdateLoop() = updateLoop.stop()

    /** Whether a loop started by [startUpdateLoop] is running. */
    fun isUpdateLoopRunning(): Boolean = updateLoop.isRunning

    /**
     * The throwable that ended the loop, or null while it has not failed.
     *
     * A callback that throws stops the loop and leaves the cause here, so a stopped loop can be
     * told apart from a quiet stream.
     */
    fun updateLoopFailure(): Throwable? = updateLoop.failure

    /** The one background loop this client owns; the implementation creates it. */
    val updateLoop: UpdateLoop
}
