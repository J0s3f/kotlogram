# Changelog

All notable changes to kotlogramme, the Kotlin/JVM facade for Telegram built on [grammers
0.8.1](https://codeberg.org/Lonami/grammers).

The release process is tag-driven: publishing a `v<version>` tag builds the bundled native
libraries and publishes `io.github.j0s3f:kotlogramme` to Maven Central. See
[`docs/publishing.md`](docs/publishing.md).

## 0.1.0 — unreleased

First compatibility release, 0.1.0 is the WIP surface that closes the Phase 1 gap to the grammers
typed client API. Sixty-six native operations, each reachable from Kotlin.

### Bridge

- Per-domain bridge split under `org.kotlogramme` with one `@Operation`-annotated interface per
  domain, a shared `Transport`, and a drift gate that keeps `native/operations.txt`, the Rust
  dispatcher and the Kotlin annotations identical.
- The Kotlogram-shaped facade in `com.github.badoualy.telegram.api` now composes all eleven
  domains (`AuthApi`, `MessagesApi`, `ChatsApi`, `DialogsApi`, `UpdatesApi`, `MediaApi`,
  `FilesApi`, `InlineApi`, `ActionsApi`, `MarkupApi`, `RawApi`).
- Rich entity projections: `User` (27 fields), `Message` (24), `Peer` (10), `Dialog` (9),
  `Participant` (11), flat `Media` with a `kind` discriminator, `ChatPermissions`,
  `ChatRestrictions`, typed update payloads with update state and the raw TL update, reply
  markups with every button kind, and dialog metadata.
- `Transport.decode` accepts a bare JSON `null` as a document, so null results (`getPinnedMessage`,
  `getReplyMarkup`) round-trip instead of surfacing as errors.

### Auth and identity

- `authLogOut`, the full `getAccountIdentity` (account plus data centre) and `getDataCentreId`.

### Messages

- History paging and totals, search with filters, date bounds and `sentBySelf`, global search with
  totals, reply-to resolution, and chat photos via the chat-photos message filter.

### Media and files

- Typed media send (`mediaSend`, `mediaSendUrl`, `mediaCopy`) with plain/HTML/Markdown captions,
  spoiler, MIME override, self-destructing TTL, `invertMedia` and scheduling.
- `downloadMedia`, chunked `downloadMediaChunk` (with seek-by-`skipChunks`), `uploadFile` metadata
  and `getProfilePhotos`. The `html`/`markdown` grammers features are enabled and `mime_guess`
  added for the spoiler path.

### Chats and dialogs

- Participant permissions, banned/admin rights (`channelsEditBanned`, `channelsEditAdmin`),
  invite-link accept and parse, `channelsResolvePeer` by Bot API id, and participant filters with
  totals.
- Dialog metadata off the raw layer payload, dialog totals and `messagesClearMentions`.

### Updates, inline, markup and actions

- `getNextTypedUpdate` / `getNextRawUpdate` / `syncUpdateState` and the on-demand
  `dispatchNextUpdate` through the legacy `UpdateCallback`.
- `inlineQuery`, `answerCallbackQuery`, `answerInlineQuery`, `editInlineMessage`.
- The four reply-markup builders and reading a message's markup.
- Chat actions (`actionsSendChatAction`, `actionsCancelChatAction`) and the service action of a
  message.

### Reliability

- Rust wire-contract tests and Kotlin projection tests on both sides of every projection.
- The seven frozen JNI export names are regression-tested against the release DLL.
- The live integration test gained a media/file round-trip scenario (opt-in, not run in CI).

### Notes and known limitations

- The historical generated Layer-66 TL API is not recreated; a versioned raw API
  (`RawTelegramApi`) is the escape hatch for methods the facade does not map.
- grammers 0.8.1 exposes no typed contacts or account API, so those families stay behind
  `invokeRaw`.
- `UpdateCallback` is delivered on demand by the caller's loop; there is no background dispatcher.
- `accept_invite_link`/`parse_invite_link` are rebuilt on the same requests because the grammers
  feature is not enabled; date-bounded search invokes `messages.Search` directly because grammers
  does not re-export the `chrono` type its iterator filters want.