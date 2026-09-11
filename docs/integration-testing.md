# Live Telegram integration test

`LiveTelegramIntegrationTest` proves the public compatibility facade against Telegram itself. It authorizes a bot, verifies an already-authorized user session, has the user join a public test supergroup, sends a unique message, and polls the bot's channel history until that message is received.

It is deliberately excluded from `test` and GitHub Actions. The test causes real Telegram activity and must use dedicated, disposable test accounts.

## Test chat requirements

Create a public **supergroup** rather than a broadcast-only channel. Its members must be allowed to send messages. Add the test bot to it in advance and disable the bot's privacy mode with BotFather, so the bot can access ordinary group messages. Do not use a production or private chat.

The user session must already be authorized for the same API ID and API hash. Telegram phone codes cannot safely be automated in a non-interactive test. Keep that SQLite session outside the repository; it contains authentication material.

## Running the test

Set these environment variables:

- `KOTLOGRAMME_RUN_LIVE_TESTS=true`
- `KOTLOGRAMME_TEST_API_ID`
- `KOTLOGRAMME_TEST_API_HASH`
- `KOTLOGRAMME_TEST_BOT_TOKEN`
- `KOTLOGRAMME_TEST_USER_SESSION` — absolute path to the pre-authorized user SQLite session
- `KOTLOGRAMME_TEST_CHANNEL_USERNAME` — public supergroup username, with or without `@`

Make a matching native library available, either by running from the packaged JAR or by setting `-Dkotlogramme.native.path` to a locally built library for the current platform. Then run:

```text
gradle :kotlogramme-kotlin:integrationTest
```

The test creates a temporary bot session and removes it after completion. It does not delete the user session or the message posted to the dedicated test supergroup.
