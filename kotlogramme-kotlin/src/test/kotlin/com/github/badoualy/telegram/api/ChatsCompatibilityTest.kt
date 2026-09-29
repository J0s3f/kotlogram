package com.github.badoualy.telegram.api

import kotlinx.serialization.json.Json
import org.kotlogramme.protocol.Participant as BridgeParticipant
import org.kotlogramme.protocol.ParticipantPermissions as BridgeParticipantPermissions
import org.kotlogramme.protocol.ParticipantsResult as BridgeParticipantsResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests that the compatibility facade carries the chat-moderation projection the bridge gained,
 * including the direction of the ban flags.
 *
 * The documents are the same ones `ChatsProtocolTest` pins on the wire side, so a field added to
 * the native projection without reaching this layer fails here.
 */
class ChatsCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a participant page projects the listing and the total`() {
        val page = json.decodeFromString<BridgeParticipantsResult>(PARTICIPANTS).toCompatibility()

        assertEquals(1, page.participants.size)
        assertEquals(7, page.participants[0].user.id)
        assertEquals("member", page.participants[0].role)
        assertEquals(4_825, page.total)
    }

    @Test
    fun `participant permissions keep every flag`() {
        val permissions =
            json.decodeFromString<BridgeParticipantPermissions>(PERMISSIONS).toCompatibility()

        assertTrue(permissions.isCreator && permissions.isAdmin && permissions.canAddAdmins)
        assertTrue(!permissions.isBanned && !permissions.hasLeft && !permissions.hasDefaultPermissions)
    }

    @Test
    fun `a banned participant keeps the denied flags the layer reported`() {
        val participant = json.decodeFromString<BridgeParticipant>(BANNED).toCompatibility()

        assertEquals("banned", participant.role)
        // `sendMedia` is true on the wire because the layer *denies* it; the facade keeps that
        // polarity rather than inverting it into a "can do" flag.
        assertTrue(participant.restrictions?.sendMedia == true)
        assertTrue(participant.restrictions?.viewMessages == true)
        assertTrue(participant.restrictions?.sendMessages == false)
        assertEquals(1_700_000_000_000, participant.restrictions?.untilDate)
    }

    @Test
    fun `a requested ban crosses to the bridge with the same polarity`() {
        val restrictions = ChatRestrictions(
            viewMessages = false,
            sendMessages = true,
            sendMedia = false,
            sendStickers = true,
            sendGifs = false,
            sendGames = false,
            sendInline = false,
            embedLinks = false,
            sendPolls = false,
            changeInfo = false,
            inviteUsers = false,
            pinMessages = false,
            untilDate = 1_700_000_000_000,
        )

        val bridge = restrictions.toBridge()

        assertTrue(bridge.sendMessages && bridge.sendStickers)
        assertTrue(!bridge.viewMessages && !bridge.sendMedia)
        assertEquals(1_700_000_000_000, bridge.untilDate)
    }

    @Test
    fun `a requested admin grant crosses to the bridge unchanged`() {
        val permissions = ChatPermissions(
            changeInfo = true,
            postMessages = false,
            editMessages = true,
            deleteMessages = false,
            banUsers = true,
            inviteUsers = false,
            pinMessages = true,
            addAdmins = false,
            anonymous = true,
            manageCall = false,
        )

        val bridge = permissions.toBridge()

        assertTrue(bridge.changeInfo && bridge.editMessages && bridge.banUsers && bridge.pinMessages)
        assertTrue(bridge.anonymous)
        assertTrue(!bridge.postMessages && !bridge.deleteMessages && !bridge.inviteUsers)
        assertTrue(!bridge.addAdmins && !bridge.manageCall)
    }

    @Test
    fun `the participant filters name the wire filters the bridge sends`() {
        assertNull(ParticipantFilter.Recent.wire)
        assertEquals("admins", ParticipantFilter.Admins.wire)
        assertEquals("bots", ParticipantFilter.Bots.wire)
        assertEquals("banned", ParticipantFilter.Banned.wire)
        assertEquals("contacts", ParticipantFilter.Contacts.wire)
        assertEquals("search", ParticipantFilter.Search.wire)
        assertEquals("kicked", ParticipantFilter.Kicked.wire)
    }

    private companion object {
        val PARTICIPANTS = """
            {"participants": [{"user": {"id": 7}, "role": "member"}], "total": 4825}
        """.trimIndent()

        val PERMISSIONS = """
            {"isCreator": true, "isAdmin": true, "isBanned": false, "hasLeft": false,
             "hasDefaultPermissions": false, "canAddAdmins": true}
        """.trimIndent()

        val BANNED = """
            {"user": {"id": 7}, "role": "banned",
             "restrictions": {"viewMessages": true, "sendMessages": false, "sendMedia": true,
                              "sendStickers": false, "sendGifs": false, "sendGames": false,
                              "sendInline": false, "embedLinks": false, "sendPolls": false,
                              "changeInfo": false, "inviteUsers": false, "pinMessages": false,
                              "untilDate": 1700000000000}}
        """.trimIndent()
    }
}
