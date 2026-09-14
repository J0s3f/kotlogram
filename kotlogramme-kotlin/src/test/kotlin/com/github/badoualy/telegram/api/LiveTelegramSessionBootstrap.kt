package com.github.badoualy.telegram.api

import java.nio.file.Files
import java.nio.file.Path

/**
 * Creates the persistent user session consumed by [LiveTelegramIntegrationTest].
 *
 * This program is intentionally interactive: Telegram login and 2FA codes must never be stored
 * in environment variables, repository files, or CI configuration.
 */
object LiveTelegramSessionBootstrap {
    @JvmStatic
    fun main(args: Array<String>) {
        val apiId = requiredEnvironment("KOTLOGRAMME_TEST_API_ID").toInt()
        val apiHash = requiredEnvironment("KOTLOGRAMME_TEST_API_HASH")
        val phoneNumber = requiredEnvironment("KOTLOGRAMME_TEST_USER_PHONE")
        val sessionPath = Path.of(requiredEnvironment("KOTLOGRAMME_TEST_USER_SESSION")).toAbsolutePath()
        sessionPath.parent?.let(Files::createDirectories)

        Kotlogram.getDefaultClient(
            TelegramApp(apiId, apiHash),
            FileTelegramApiStorage(sessionPath),
        ).use { user ->
            if (user.isAuthorized()) {
                println("The test-user session is already authorized: $sessionPath")
                return
            }

            user.authSendCode(phoneNumber = phoneNumber)
            val code = readRequiredLine("Enter the Telegram login code for $phoneNumber: ")
            try {
                user.authSignIn(phoneNumber, "managed-by-kotlogramme", code)
            } catch (_: PasswordRequiredException) {
                val password = readRequiredLine("Enter the Telegram 2FA password: ")
                user.authCheckPassword(password)
            }
            check(user.isAuthorized()) { "Telegram did not authorize the test-user session" }
            println("Authorized test-user session written to: $sessionPath")
        }
    }

    private fun requiredEnvironment(name: String): String =
        requireNotNull(System.getenv(name)?.takeIf(String::isNotBlank)) { "$name must be set" }

    private fun readRequiredLine(prompt: String): String {
        print(prompt)
        return requireNotNull(readlnOrNull()?.trim()?.takeIf(String::isNotBlank)) { "A value is required" }
    }
}
