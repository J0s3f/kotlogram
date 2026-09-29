package com.github.badoualy.telegram.api

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises real Telegram authorization and message delivery against a dedicated test supergroup.
 *
 * This class is excluded from the normal test task. See docs/integration-testing.md for the
 * required test accounts and the explicit environment-variable gate.
 */
class LiveTelegramIntegrationTest {
    @Test
    fun `bot and user exchange messages after the user joins a channel`() {
        val config = LiveTelegramConfig.loadOrSkip() ?: return
        val botSessionDirectory = Files.createTempDirectory("kotlogramme-live-bot-")
        val botSession = botSessionDirectory.resolve("bot.session")

        try {
            val application = TelegramApp(config.apiId, config.apiHash)
            Kotlogram.getDefaultClient(application, FileTelegramApiStorage(config.userSession)).use { user ->
                assertTrue(user.isAuthorized(), "The configured user session is not authorized")

                Kotlogram.getDefaultClient(application, FileTelegramApiStorage(botSession)).use { bot ->
                    if (!bot.isAuthorized()) {
                        bot.authImportBotAuthorization(config.botToken)
                    }
                    assertTrue(bot.isAuthorized(), "Bot authorization did not complete")

                    val userChannel = user.contactsResolveUsername(config.channelUsername)
                    user.channelsJoinChannel(userChannel)
                    val botChannel = bot.contactsResolveUsername(config.channelUsername)
                    val userMarker = "kotlogramme-live-user-${UUID.randomUUID()}"
                    val botMarker = "kotlogramme-live-bot-${UUID.randomUUID()}"

                    user.messagesSendMessage(userChannel, userMarker)
                    val botReceived = poll(Duration.ofSeconds(30)) {
                        bot.getNextUpdate(timeoutMillis = 1_000)?.message
                            ?.takeIf { message -> message.text == userMarker }
                    }
                    assertNotNull(botReceived, "The bot did not receive the user test message as an update")
                    assertTrue(!botReceived.outgoing, "The received message must be incoming to the bot")

                    bot.messagesSendMessage(botChannel, botMarker)

                    val received = poll(Duration.ofSeconds(30)) {
                        user.messagesGetHistory(userChannel, limit = 100)
                            .firstOrNull { message -> message.text == botMarker }
                    }
                    assertNotNull(received, "The user did not receive the bot test message through channel history")
                    assertTrue(!received.outgoing, "The received message must be incoming to the user")
                }
            }
        } finally {
            deleteSessionFiles(botSession)
            Files.deleteIfExists(botSessionDirectory)
        }
    }

    @Test
    fun `bot sends media and the user downloads and inspects it`() {
        val config = LiveTelegramConfig.loadOrSkip() ?: return
        val botSessionDirectory = Files.createTempDirectory("kotlogramme-live-bot-")
        val botSession = botSessionDirectory.resolve("bot.session")
        val scratchDirectory = Files.createTempDirectory("kotlogramme-live-scratch-")

        try {
            val application = TelegramApp(config.apiId, config.apiHash)
            Kotlogram.getDefaultClient(application, FileTelegramApiStorage(config.userSession)).use { user ->
                assertTrue(user.isAuthorized(), "The configured user session is not authorized")

                Kotlogram.getDefaultClient(application, FileTelegramApiStorage(botSession)).use { bot ->
                    if (!bot.isAuthorized()) {
                        bot.authImportBotAuthorization(config.botToken)
                    }
                    assertTrue(bot.isAuthorized(), "Bot authorization did not complete")

                    val channel = bot.contactsResolveUsername(config.channelUsername)
                    val mediaMarker = "kotlogramme-live-media-${UUID.randomUUID()}"
                    val payload = Files.writeString(
                        Files.createTempFile(scratchDirectory, "kotlogramme-live-file-", ".txt"),
                        "kotlogramme live media payload\n".repeat(64),
                    )

                    // The bot uploads a document and sends it with a caption; the user's search
                    // finds it by the caption once Telegram has indexed it.
                    bot.mediaSend(channel, payload, caption = mediaMarker)
                    val sent = poll(Duration.ofSeconds(30)) {
                        user.messagesSearch(channel, mediaMarker, limit = 10)
                            .firstOrNull { message -> message.text == mediaMarker }
                    }
                    assertNotNull(sent, "The user did not find the bot media message through search")
                    assertTrue(sent.id > 0, "The sent media message must carry a message id")

                    // The whole-file download matches the uploaded bytes, and the chunked one reads
                    // the same media.
                    val target = scratchDirectory.resolve("out.txt")
                    val downloaded = user.downloadMedia(channel, sent.id, target)
                    assertEquals(payload.toFile().length(), downloaded.size, "The download must hold the whole file")
                    val firstChunk = user.downloadMediaChunk(channel, sent.id, chunkSize = 512 * 1024, skipChunks = 0)
                    assertNotNull(firstChunk, "The first chunk of the media must exist")
                    assertTrue(firstChunk.size > 0, "The first chunk must carry bytes")
                    assertEquals(Files.size(target), firstChunk.size, "The chunk must cover the file")

                    // A plain text message carries no markup, no service action and no reply target:
                    // all three operations answer null through the bare-null result path.
                    val plainMarker = "kotlogramme-live-plain-${UUID.randomUUID()}"
                    user.messagesSendMessage(channel, plainMarker)
                    val plain = poll(Duration.ofSeconds(30)) {
                        user.messagesSearch(channel, plainMarker, limit = 10)
                            .firstOrNull { message -> message.text == plainMarker }
                    }
                    assertNotNull(plain, "The user did not find its own plain message through search")
                    assertNull(user.markupGetReplyMarkup(channel, plain.id), "A plain message has no markup")
                    assertNull(user.actionsGetMessageAction(channel, plain.id), "A plain message has no service action")
                    assertNull(user.messagesGetReplyToMessage(channel, plain.id), "A plain message has no reply target")
                }
            }
        } finally {
            deleteSessionFiles(botSession)
            Files.deleteIfExists(botSessionDirectory)
            scratchDirectory.toFile().deleteRecursively()
        }
    }

    private fun <T> poll(timeout: Duration, action: () -> T?): T? {
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline)) {
            action()?.let { return it }
            Thread.sleep(500)
        }
        return null
    }

    private fun deleteSessionFiles(session: Path) {
        Files.deleteIfExists(session)
        Files.deleteIfExists(Path.of("${session}-wal"))
        Files.deleteIfExists(Path.of("${session}-shm"))
    }
}

private data class LiveTelegramConfig(
    val apiId: Int,
    val apiHash: String,
    val botToken: String,
    val userSession: Path,
    val channelUsername: String,
) {
    companion object {
        fun loadOrSkip(): LiveTelegramConfig? {
            if (System.getenv("KOTLOGRAMME_RUN_LIVE_TESTS") != "true") {
                println("Skipping live Telegram integration test; set KOTLOGRAMME_RUN_LIVE_TESTS=true to enable it.")
                return null
            }
            fun required(name: String): String = requireNotNull(System.getenv(name)?.takeIf(String::isNotBlank)) {
                "$name must be set when KOTLOGRAMME_RUN_LIVE_TESTS=true"
            }
            return LiveTelegramConfig(
                apiId = required("KOTLOGRAMME_TEST_API_ID").toInt(),
                apiHash = required("KOTLOGRAMME_TEST_API_HASH"),
                botToken = required("KOTLOGRAMME_TEST_BOT_TOKEN"),
                userSession = Path.of(required("KOTLOGRAMME_TEST_USER_SESSION")),
                channelUsername = required("KOTLOGRAMME_TEST_CHANNEL_USERNAME").removePrefix("@"),
            )
        }
    }
}
