# Live Telegram integration test

`LiveTelegramIntegrationTest` proves the public compatibility facade against Telegram itself. It authorizes a bot, verifies an already-authorized user session, has the user join a public test supergroup, sends a unique user message, verifies that the bot receives it through `getNextUpdate`, sends a unique bot reply, and polls the user's channel history until that reply is received. Telegram does not allow bot MTProto sessions to call `messages.getHistory`, so bot input is asserted through the ordered update stream instead.

It is deliberately excluded from `test` and GitHub Actions. The test causes real Telegram activity and must use dedicated, disposable test accounts.

## Test chat requirements

Create a public **supergroup** rather than a broadcast-only channel. Its members must be allowed to send messages. Add the test bot to it in advance and disable the bot's privacy mode with BotFather, so the bot can access ordinary group messages. Do not use a production or private chat.

The user session must be authorized for the same API ID and API hash. Telegram phone codes cannot safely be put in a non-interactive test. Keep that SQLite session outside the repository; it contains authentication material.

## Running the test

Run Gradle with JDK 21. The pinned Kotlin build tooling does not yet support JDK 25 as its launcher JVM. The repository includes `gradlew` / `gradlew.bat`, so no global Gradle installation is required.

Set these environment variables:

- `KOTLOGRAMME_RUN_LIVE_TESTS=true`
- `KOTLOGRAMME_TEST_API_ID`
- `KOTLOGRAMME_TEST_API_HASH`
- `KOTLOGRAMME_TEST_BOT_TOKEN`
- `KOTLOGRAMME_TEST_USER_SESSION` — absolute path to the pre-authorized user SQLite session
- `KOTLOGRAMME_TEST_USER_PHONE` — phone number used once to bootstrap that session
- `KOTLOGRAMME_TEST_CHANNEL_USERNAME` — public supergroup username, with or without `@`

Make a matching native library available by setting `-Dkotlogramme.native.path` to a locally built library for the current platform. Bootstrap the session once; the program asks for the Telegram login code and optional 2FA password in the terminal rather than reading either from an environment variable:

```text
./gradlew -Dkotlogramme.native.path=/absolute/path/to/native-library :kotlogramme-kotlin:authorizeLiveTestUser
```

Then run:

```text
./gradlew :kotlogramme-kotlin:integrationTest
```

The test creates a temporary bot session and removes it after completion. It does not delete the user session or the message posted to the dedicated test supergroup.
