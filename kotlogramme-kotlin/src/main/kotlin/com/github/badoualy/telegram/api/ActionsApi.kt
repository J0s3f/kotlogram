package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.MessageAction

/**
 * The chat-action statuses Telegram shows next to a chat, named as the layer names them.
 *
 * [wireName] is what the native bridge sends; [takesProgress] mirrors the statuses the layer
 * carries a percentage for, so an uploading status can report how far it is along and the others
 * reject one.
 */
enum class ChatAction(val wireName: String, val takesProgress: Boolean = false) {
    /** The sender is composing a message. */
    TYPING("typing"),

    /** The sender is uploading a photo. */
    UPLOAD_PHOTO("uploadPhoto", takesProgress = true),

    /** The sender is uploading a file. */
    UPLOAD_DOCUMENT("uploadDocument", takesProgress = true),

    /** The sender is recording a video. */
    RECORD_VIDEO("recordVideo"),

    /** The sender is uploading a video. */
    UPLOAD_VIDEO("uploadVideo", takesProgress = true),

    /** The sender is recording a voice message. */
    RECORD_VOICE("recordVoice"),

    /** The sender is uploading a voice message. */
    UPLOAD_VOICE("uploadVoice", takesProgress = true),

    /** The sender is recording a video note. */
    RECORD_VIDEO_NOTE("recordVideoNote"),

    /** The sender is uploading a video note. */
    UPLOAD_VIDEO_NOTE("uploadVideoNote", takesProgress = true),

    /** The sender is choosing a sticker. */
    CHOOSE_STICKER("chooseSticker"),

    /** The sender is sharing a contact. */
    CHOOSE_CONTACT("chooseContact"),

    /** The sender is sharing a location. */
    GEO_LOCATION("geoLocation"),

    /** The sender is playing a game. */
    GAME_PLAY("gamePlay"),

    /** The sender is importing a chat history. */
    HISTORY_IMPORT("historyImport", takesProgress = true),

    /** The sender is speaking in a group call. */
    SPEAKING_IN_GROUP_CALL("speakingInGroupCall"),

    /** Clears whatever status the account is showing in the chat. */
    CANCEL("cancel"),
}

/** Reporting the typing, uploading and recording status of a chat, and reading a service action. */
interface ActionsApi : BridgeApi {
    /**
     * Sends the one-shot chat action [action], the status Telegram shows next to the chat.
     *
     * [progress] is the percentage the uploading statuses report and must be left out otherwise;
     * [topicId] targets one forum topic instead of the whole chat.
     */
    fun actionsSendChatAction(
        peer: TelegramPeer,
        action: ChatAction,
        progress: Int? = null,
        topicId: Int? = null,
    ) {
        require(progress == null || action.takesProgress) {
            "${action.wireName} does not take a progress percentage"
        }
        bridge.sendChatAction(peer.native, action.wireName, progress, topicId)
    }

    /** Clears the account's chat action in [peer], optionally in one forum topic. */
    fun actionsCancelChatAction(peer: TelegramPeer, topicId: Int? = null) {
        bridge.sendChatAction(peer.native, ChatAction.CANCEL.wireName, null, topicId)
    }

    /**
     * The service action of a message, or null when the message is an ordinary one.
     *
     * [MessageAction.kind] names the action; the layer's own payload for it is not projected.
     */
    fun actionsGetMessageAction(peer: TelegramPeer, messageId: Int): MessageAction? =
        bridge.getMessageAction(peer.native, messageId)
}
