package com.github.badoualy.telegram.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests that the facade's chat-action vocabulary matches what the native bridge accepts.
 *
 * The wire names are the contract: `native/src/ops/actions.rs` maps each of them onto the layer's
 * own status variant and rejects anything else, so a name renamed or invented here would only fail
 * against a live session.
 */
class ActionsApiTest {
    @Test
    fun `every status has the wire name the native bridge maps`() {
        assertEquals(
            mapOf(
                "typing" to ChatAction.TYPING,
                "uploadPhoto" to ChatAction.UPLOAD_PHOTO,
                "uploadDocument" to ChatAction.UPLOAD_DOCUMENT,
                "recordVideo" to ChatAction.RECORD_VIDEO,
                "uploadVideo" to ChatAction.UPLOAD_VIDEO,
                "recordVoice" to ChatAction.RECORD_VOICE,
                "uploadVoice" to ChatAction.UPLOAD_VOICE,
                "recordVideoNote" to ChatAction.RECORD_VIDEO_NOTE,
                "uploadVideoNote" to ChatAction.UPLOAD_VIDEO_NOTE,
                "chooseSticker" to ChatAction.CHOOSE_STICKER,
                "chooseContact" to ChatAction.CHOOSE_CONTACT,
                "geoLocation" to ChatAction.GEO_LOCATION,
                "gamePlay" to ChatAction.GAME_PLAY,
                "historyImport" to ChatAction.HISTORY_IMPORT,
                "speakingInGroupCall" to ChatAction.SPEAKING_IN_GROUP_CALL,
                "cancel" to ChatAction.CANCEL,
            ),
            ChatAction.entries.associateBy { it.wireName },
        )
    }

    @Test
    fun `only the uploading statuses take a progress percentage`() {
        assertEquals(
            listOf(
                ChatAction.UPLOAD_PHOTO,
                ChatAction.UPLOAD_DOCUMENT,
                ChatAction.UPLOAD_VIDEO,
                ChatAction.UPLOAD_VOICE,
                ChatAction.UPLOAD_VIDEO_NOTE,
                ChatAction.HISTORY_IMPORT,
            ),
            ChatAction.entries.filter { it.takesProgress },
        )
        assertFalse(ChatAction.TYPING.takesProgress)
        assertFalse(ChatAction.CANCEL.takesProgress)
    }
}
