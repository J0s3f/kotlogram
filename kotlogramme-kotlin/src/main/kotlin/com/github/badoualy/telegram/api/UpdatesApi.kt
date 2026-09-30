package com.github.badoualy.telegram.api

/**
 * Consuming grammers' ordered update stream.
 *
 * [getNextUpdate] is the pair of fields the facade started with. [getNextTypedUpdate] is the same
 * call with the whole projection, and [getNextRawUpdate] is the same call again with the update
 * exactly as Telegram sent it; all three read one stream in turn rather than each holding their
 * own, so consuming them alternates between the typed and the untyped view of the same updates.
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
}
