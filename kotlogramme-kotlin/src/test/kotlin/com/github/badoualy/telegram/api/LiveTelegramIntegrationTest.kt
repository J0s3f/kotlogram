package com.github.badoualy.telegram.api

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNotNull
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
