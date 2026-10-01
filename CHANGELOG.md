# Changelog

All notable changes to kotlogramme, the Kotlin/JVM facade for Telegram built on
[grammers](https://codeberg.org/Lonami/grammers) (tracked from its codeberg repository).

The release process is tag-driven: publishing a `v<version>` tag builds the bundled native
libraries and publishes `io.github.j0s3f:kotlogramme` to Maven Central. See
[`docs/publishing.md`](docs/publishing.md).

## 0.8.0 - unreleased

### Added

- `FilesApi.uploadStream` is now genuinely incremental: the bytes cross a bounded native channel to
  grammers as they are read, instead of being collected in one `Vec<u8>` until the finish call, so
  the memory an upload holds is a few chunks whatever the file size. It takes the stream's total
  `size` and an optional `onProgress` callback, because grammers has to know the size before it can
  send the first part.
- `FilesApi.uploadProgressBegin(total)` and `FilesApi.uploadProgress(handle)` report an upload while
  it runs: bytes sent, the total, elapsed milliseconds and the average bytes per second, which is
  the shape a terminal render loop polls. `uploadFile(path)` and a path `MediaApi.mediaSend` accept
  an optional `progressHandle` from `uploadProgressBegin` and count their local file into it through
  the same counting reader, so a path upload can be observed from another thread although its own
  call blocks. 101 operations.

### Changed

- `FilesApi.uploadStream` takes the stream's total `size`. The previous three-argument form could
  not report progress and buffered the whole file; a caller moves by passing the size it already
  knows (a file's length, or an HTTP `Content-Length`).

## 0.7.0 - 2026-09-30

### Added

- received messages now carry their formatting entities and their rendered text: `Message.entities`
  exposes each entity's `type`, `offset`, `length` and the extra its kind carries (`url`, `userId`,
  `language` or `customEmojiId`), and `Message.htmlText` / `Message.markdownText` are the same text
  rendered by grammers. A terminal client can now tell bold from plain, and an entity read back can
  be handed straight to a send or edit.
- `UsersApi.usersGetUsers(ids)` - resolves numeric user ids to accounts, the missing half that turns
  a message's bare `viaBotId` into the bot behind it. The answer reuses the contacts projection
  (`User` plus a registered peer), deduplicates repeated ids and omits an id Telegram does not
  resolve. grammers has no typed users-by-id helper, so the operation builds the layer's
  `users.GetUsers` request directly, filling each id's access hash from the session cache. 99
  operations.

## 0.6.0 - 2026-09-30

### Changed

- `Media.kind` now names a received document by what it carries: `animation` when it is animated,
  `video` for a `video/*` MIME type, `audio` for `audio/*`, and `document` otherwise. The layer
  reports `messageMediaDocument` for all of them, so a video used to arrive as `document` and a
  terminal client could not tell a 43-minute video from a text file. The send-side kinds
  (`photo`/`document`/`file`/`video`) are unchanged.

## 0.5.0 — 2026-09-30

### Added

- `MediaKind.VIDEO` on `MediaApi.mediaSend` — sends a local file as a **streamable video**. Telegram
  plays a video in place only when the document carries a `DocumentAttributeVideo` with
  `supports_streaming`, and grammers' `photo`/`document`/`file` builders cannot add attributes, so
  the video document is always built raw. `durationSeconds`, `width` and `height` describe it, and
  default to 0 when the caller does not know them.

## 0.4.0 — 2026-09-30

### Added

- `StickersApi.messagesSendSticker(peer, shortName | id/accessHash, index, …)` — sends one sticker
  of a set. The set is fetched first and the document at `index` becomes the layer's
  `inputMediaDocument`, file reference and all, exactly as grammers' own `Media::to_raw_input_media`
  does, so the reference never crosses the JSON boundary where it would go stale. 98 operations.

## 0.3.0 — 2026-09-30

### Added

- `ChatsApi.channelsInviteToChannel(peer, user)` — adds a member to a channel, supergroup or basic
  group. The facade could join, leave, list and kick but nothing added a member, which the live-test
  client hit immediately. grammers has no invite method, so the layer's requests are written
  directly (`channels.InviteToChannel` / `messages.AddChatUser`) with the same peer-kind branching
  the banned-rights path uses. 97 operations.

### Fixed

- Dialog folders no longer fail with `CHANNEL_PRIVATE` when a folder references a channel the
  account cannot access; the projection skips the peer it cannot resolve instead of losing the whole
  listing.

## 0.2.0 — 2026-09-30

Second release. It keeps the 0.1.0 parity surface, adds the post-parity feature work below, and
aligns the raw API to Telegram TL layer 229. Ninety-six native operations, each reachable from
Kotlin.

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

### Post-parity feature work

- **Chat rights, full round-trip.** All Layer-229 admin and ban flags are projected and written; the
  rights the grammers builders cannot set go through raw `channels.EditBanned` / `channels.EditAdmin`
  (or the basic-group `messages.EditChatAdmin` / `DeleteChatUser`), merging the current rights via
  `channels.GetParticipant`.
- **Reply markups can finally be sent.** A shared `MarkupSpec` is accepted by every send payload
  (`messagesSendMessage`, `messagesSendFile`, `mediaSend`, `mediaSendUrl`, `mediaCopy`) and both
  edits, with `ReplyMarkup.asSpec()` to send a markup read off another message.
- **Full edit parity.** `messagesEditMessage` and `editInlineMessage` carry text, parse modes,
  explicit entities, reply markup, media replacement (path / URL / copy of another message) and TTL.
- **Richer messages.** `Message` gained forward and reply headers, restriction reasons, the embedded
  service action and reply markup, and full peer/sender objects.
- **Guest chats.** The `GuestChatQuery` update is projected and `answerGuestChatQuery` answers it
  with `messages.SetBotGuestChatResult`.
- **Inline.** `sendInlineBotResult` sends a chosen result; the query page reports `switchPm`,
  `switchWebview` and the text a result would post; `answerInlineQuery` gained media results
  (photo/gif/video/voice/document) beside articles.
- **Uploads.** `uploadBytes` and the `uploadStreamBegin`/`Chunk`/`Finish` trio upload from memory or
  a stream, and every send references an upload by `fileHandle` instead of re-reading a path.
- **Typed raw families.** `ContactsApi`, `AccountApi`, `FoldersApi` and `StickersApi` expose the
  operations grammers has no high-level API for — contacts and blocking, profile/username/status,
  authorizations, password settings and privacy rules, dialog filters, and sticker-set reads — as
  typed operations over the TL layer.

### Fixes

- The raw-API contract now matches the pinned grammers revision: the bundled
  `kotlogram-raw-schema/v1` manifest is Layer 229, and `Kotlogram.API_LAYER` and
  `RawTelegramApi.LAYER` report the same value. The CI schema-drift gate now locates `api.tl`
  under the git checkout, because the old `registry/src` path no longer applies now that
  grammers is a git dependency.

### Notes and known limitations

- The bridge tracks grammers' codeberg repository rather than a crates.io version, because grammers
  is developed there and releases are cut rarely. The three grammers crates are pinned to rev
  `42d4b51059524cf7c8c9a07745a1071057fa1526` for reproducible builds.
- The historical generated Layer-66 TL API is not recreated; a versioned raw API
  (`RawTelegramApi`) is the escape hatch for methods the facade does not map.
- grammers exposes no typed contacts or account API; the `ContactsApi` and `AccountApi` families
  are hand-written over the TL layer rather than grammers client methods.
- `UpdateCallback` is delivered on demand by the caller's loop; there is no background dispatcher.
- `accept_invite_link`/`parse_invite_link` and `Client::edit_inline_message` are rebuilt on the
  same layer requests because the grammers feature is crate-private or not enabled; date-bounded
  search is back on grammers' own `SearchIter` via a `jiff::Timestamp`.