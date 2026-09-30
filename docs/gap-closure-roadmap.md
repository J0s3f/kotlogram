# Gap-closure roadmap

Plan of record for closing every feature gap identified after the grammers 0.10.0 (git) port.
Companion to [`grammers-parity-plan.md`](grammers-parity-plan.md), which covers the work already
delivered. Nothing here has started; each task becomes one subagent, one worktree, one branch.

## Locked decisions

Answers given on 2026-09-30, in effect for every task below:

1. **grammers pinned by `rev`** on the same codeberg repository
   (`42d4b51059524cf7c8c9a07745a1071057fa1526`, the master HEAD the 0.10.0 port was built against).
2. **Markup attach**: a tagged markup spec becomes an optional field on every send and edit payload;
   the Kotlin `ReplyMarkup` model gains a spec conversion.
3. **Edit parity**: `editMessage`/`editInlineMessage` reach full `InputMessage` parity — text,
   parse modes, explicit entities, markup, media replacement, TTL, `invertMedia`.
4. **Message projection**: add forward/reply headers, restriction reasons, embedded action,
   embedded reply markup and full peer/sender objects. Formatting entities and rendered
   markdown/html text stay unprojected for now.
5. **GuestChatQuery**: project the update variant and add `answerGuestChatQuery`.
6. **Inline**: add `sendInlineBotResult`, enrich the result-page projection with
   `switchPm`/`switchWebview`/`sendMessageText`, and extend `answerInlineQuery` beyond articles to
   media results (photo/gif/video/voice/document via URL, built from raw TL constructors).
7. **Uploads**: both a single-shot `uploadBytes` and a chunked `uploadStreamBegin/Chunk/Finish`;
   an upload-handle registry lets sends reference an already-uploaded file.
8. **RawApi**: the wider curated set — contacts, account, dialog filters (folders), stickers and
   peer notification settings, as typed operations over grammers' TL layer.
9. **Execution**: one worktree and branch per task, agents never touch the main worktree, the
   orchestrator merges into the integration branch and delivers one squashed commit to `main`.
10. **Order**: dependency order — enrichment and send-side composition first (they unblock edit
    parity), new operations next, the raw families last, docs and integration at the end.

## What this roadmap deliberately does not include

- Formatting-entity projection on received messages and rendered `markdownText`/`htmlText`
  (decision 4). The entity *input* spec in task T4 is designed so projection can reuse it later.
- The manifest-driven Kotlin code generator for the raw API. The curated families in task T8 are
  hand-written typed operations; the generator stays a future, separately-approved project.
- Any change to the seven frozen JNI export names. Every new capability is a routed operation name,
  so `native/src/lib.rs` never changes.
- The historical generated Layer-66 TL model, and a background `UpdateCallback` loop. Both stay
  out by the standing design.

## Shared conventions for every task

- **In place, no parallel types.** Enrichment adds defaulted fields to the existing public data
  classes and DTOs. `0.1.0` is WIP, but no `*V2`/`*Rich` siblings.
- **Wire polarity.** Rights fields keep the layer's own polarity (`true` bans on restrictions,
  grants on admin rights) exactly as `docs/compatibility.md` documents today.
- **Operations contract.** New operations are declared in the owning module's `OPERATIONS`, routed
  in its `route`, listed by `tools/generate_operations.py` into `native/operations.txt`, and appear
  once as an `@Operation`-annotated bridge method. Both parity tests must stay green.
- **Kotlin shape.** One bridge method per operation on `org.kotlogramme.TelegramClient`; the facade
  gains either defaulted parameters on existing methods (source-compatible) or one new `*Api.kt`
  interface plus one supertype entry on `TelegramClient` per new domain.
- **Verification per task branch**: `cargo build --manifest-path native\Cargo.toml`,
  `cargo test --manifest-path native\Cargo.toml`, `cargo build --release --manifest-path
  native\Cargo.toml`, `cargo fmt --manifest-path native\Cargo.toml`, then
  `$env:JAVA_HOME="C:\Users\micro\.jdks\liberica-21.0.6"; .\gradlew.bat --no-daemon
  "-Porg.gradle.java.installations.paths=C:\Users\micro\.jdks\liberica-17.0.15,C:\Users\micro\.jdks\liberica-21.0.6"
  test`. On the integration branch the Gradle invocation is `clean test`, never `cleanTest test`.
- **Never** run `LiveTelegramIntegrationTest` (needs real credentials). New paths are covered by
  Rust wire-contract/fixture tests and JVM unit tests only.
- **Commits**: agents commit on their task branch only; no push, rebase or amend. Before
  committing: `git update-index --really-refresh` then `git diff --stat` must be empty; stage by
  explicit path (`core.autocrlf=true`, no `.gitattributes`).

## Tasks

### T0 — Pin grammers and sweep stale docs

Prerequisite for everything: all task branches fork from the integration branch after T0 lands.

- `native/Cargo.toml`: add `rev = "42d4b51059524cf7c8c9a07745a1071057fa1526"` to the three
  grammers git dependencies; regenerate `native/Cargo.lock` and commit it.
- `README.md`, `docs/compatibility.md`: replace every "grammers 0.8.1" / "pinned
  `grammers-tl-types 0.8.0`" reference with the git-revision tracking wording; `CHANGELOG.md`
  notes the pin under Unreleased.
- Owned files: `native/Cargo.toml`, `native/Cargo.lock`, `README.md`, `docs/compatibility.md`,
  `CHANGELOG.md`.

### T1 — Full chat-rights round-trip

Today 9 admin and 14 ban flags are hardcoded `false` in both directions
(`native/src/dto/permissions.rs`). grammers 0.10.0's builders have no setters for them
(`grammers-client/src/peer/chats.rs`), so writes go raw.

- `dto/permissions.rs`: add to `ChatPermissionsDto` — `manageTopics`, `postStories`,
  `editStories`, `deleteStories`, `manageDirectMessages`, `manageRanks`, `manageLinkedPeers`,
  `manageWelcomeMessages`, `other`; to `ChatRestrictionsDto` — `manageTopics`, `sendPhotos`,
  `sendVideos`, `sendRoundvideos`, `sendAudios`, `sendVoices`, `sendDocs`, `sendPlain`,
  `editRank`, `sendReactions`, `manageLinkedPeers`. All defaulted; the `From` impls read/write the
  full layer structs.
- `dto/participant.rs`, `dto/peer.rs`: project rights from the raw TL rights structs instead of
  grammers' 10-accessor surface.
- `ops/chats.rs`: `setAdminRights`/`setBannedRights` switch to raw
  `channels.EditAdminRights`/`channels.EditBannedRights` with fully populated structs. Replicate
  the builder's peer-kind branching: megagroup/broadcast → `channels.*`; basic group →
  `messages.EditChatAdmin` (admin) / kick (ban) exactly as grammers does. Emulate
  `load_current` with `channels.GetParticipant` and merge unset fields from the current rights.
- Kotlin: `protocol/Permissions.kt` gains the defaulted fields; `ChatsApi` signatures unchanged.
- Tests: Rust round-trip tests for every new flag (the TL structs are constructible, so the
  coverage dropped in the 0.10.0 port comes back extended); Kotlin default-value and wire-polarity
  tests.
- Owned files: `native/src/dto/permissions.rs`, `native/src/dto/participant.rs`,
  `native/src/dto/peer.rs`, `native/src/ops/chats.rs`, `protocol/Permissions.kt`,
  `protocol/Participant.kt`, `protocol/Peer.kt`, matching test files, `docs/compatibility.md`
  row for rights.

### T2 — Message projection enrichment

`MessageDto` gains (all defaulted): `forwardHeader`, `replyHeader`, `restrictionReasons`,
embedded `action`, embedded `replyMarkup`, and full `peer`/`sender` objects alongside the existing
ids. `fmt_entities`, `markdownText`, `htmlText` stay out.

- `native/src/dto/message.rs`: new `ForwardHeaderDto`/`ReplyHeaderDto` mirroring grammers'
  `message::ForwardHeader`/`ReplyHeader`; reuse `RestrictionReasonDto` (move it to a shared spot if
  needed), `message_action_dto` and `reply_markup_dto`. Embedding peers requires handle
  registration, so `message_dto` takes `&NativeClient` and every caller (`dto/dialog.rs`,
  `dto/update.rs`, `ops/messages.rs`, `ops/chats.rs` search results) is updated.
- Kotlin: `protocol/Message.kt` and the compatibility `Message` data class gain the defaulted
  fields; `Models.kt` mappings extended.
- Tests: extend the existing `tl::types::Message` fixture tests per field; Kotlin mapping tests.
- Owned files: `native/src/dto/message.rs`, `native/src/dto/dialog.rs`, `native/src/dto/update.rs`
  (call sites only), `protocol/Message.kt`, `Models.kt`, test files.

### T3 — Markup attach

The four `markupBuild*` operations return a projection nothing can send. This task makes markup a
first-class send/edit input.

- Shared spec: move `InlineButtonSpec`/`KeyboardButtonSpec` out of `ops/markup.rs` into
  `payload.rs` (or `dto/markup.rs`) and add a tagged `MarkupSpec` —
  `{"kind": "inline"|"keyboard"|"forceReply"|"hide", ...}` with the same fields the four build
  payloads already accept. `ops/markup.rs` exposes `pub(crate) fn reply_markup_from_spec`.
- Payloads gain `markup: Option<MarkupSpec>`: `SendMessagePayload`, `SendFilePayload`
  (`ops/messages.rs`), `SendMediaPayload`, `SendMediaUrlPayload`, `CopyMediaPayload`
  (`ops/media.rs`). Album items do not: verify against the generated layer whether
  `messages.SendMultiMedia` carries a markup; if it does not, document the exclusion in the op.
- Kotlin: `protocol/Markup.kt` `ReplyMarkup` gains `asSpec()`; transport and facade send/edit
  methods gain a defaulted `replyMarkup: ReplyMarkup? = null` parameter.
- Tests: spec decode → TL constructor per kind; round-trip build/read; Kotlin conversion tests.
- Owned files: `native/src/ops/markup.rs`, `native/src/payload.rs`, `native/src/ops/messages.rs`
  (payload only), `native/src/ops/media.rs` (payload only), `protocol/Markup.kt`,
  `MarkupApi.kt`, `MessagesApi.kt`, `MediaApi.kt`, bridge + test files.
- Later tasks edit these payload structs again; T3 merges before T4/T5 start.

### T4 — Full edit parity (depends on T3)

- `EditMessagePayload` becomes: `text?`, `parseMode?`, `entities?`, `linkPreview?`,
  `invertMedia?`, `ttlSeconds?`, `markup?` (T3 spec), `media?: { path + kind | url + kind |
  copyOf: { peer, messageId } }`. Builds grammers' `InputMessage` accordingly; media replacement
  reuses the T3/T5 send-side media builders (`copy_media` for `copyOf`).
- Entity input spec: `{"offset", "length", "type", "url"? , "userId"?, "language"?,
  "customEmojiId"?}` — request direction only, defined so a future projection can serialize the
  same shape.
- `sendMessage` gains `parseMode`/`entities` alongside `markup` — it is plain-text-only today,
  which is the same gap on the send side.
- `EditInlineMessagePayload` (`ops/inline.rs`) gains `markup`, `entities`, `parseMode`, `media`
  (URL-only; a local path cannot be uploaded for an inline edit), on the raw
  `messages.EditInlineBotMessage` request.
- Kotlin: `messagesEditMessage`/`editInlineMessage` gain the defaulted rich parameters; the
  existing two-argument calls keep compiling unchanged.
- Owned files: `native/src/ops/messages.rs`, `native/src/ops/media.rs` (shared media-spec
  extraction), `native/src/ops/inline.rs`, `protocol/Messages.kt`, `protocol/Inline.kt`,
  `MessagesApi.kt`, `InlineApi.kt`, test files.

### T5 — Streamed and in-memory uploads (depends on T3)

Today `uploadFile` drops the `Uploaded` handle and only metadata crosses; sends always upload from
a path.

- `client.rs`: two registries — in-progress streams (`HashMap<i64, Vec<u8>>` keyed by an
  `AtomicI64` id) and finished uploads (`HashMap<i64, Uploaded>`), both per-client, dropped on
  close.
- New operations in `ops/files.rs`: `uploadBytes {name, dataBase64}` (documented size guidance —
  base64 in JSON is not for large files), `uploadStreamBegin {name}`, `uploadStreamChunk
  {uploadId, dataBase64}`, `uploadStreamFinish {uploadId}` — finish feeds the accumulated buffer
  through `Client::upload_stream` (a `std::io::Cursor` satisfies the `AsyncRead + Unpin` bound;
  the agent confirms the exact trait grammar expects) and returns `UploadedFileDto` plus a new
  defaulted `handle` field.
- Sends accept the handle: `path` XOR `fileHandle` on `sendMedia`, `sendFile` and album items,
  validated with a clear error.
- Kotlin: `FilesApi.uploadBytes(ByteArray, name)` and `FilesApi.uploadStream(InputStream, name,
  chunkSize = 512 * 1024)` running the chunk loop; `MessagesApi`/`MediaApi` gain overloads taking
  an `UploadedFile`.
- Owned files: `native/src/client.rs` (registries only), `native/src/ops/files.rs`,
  `native/src/dto/files.rs`, `native/src/ops/messages.rs` and `native/src/ops/media.rs` (the
  `fileHandle` field), `protocol/Files.kt`, `FilesApi.kt`, `MessagesApi.kt`, `MediaApi.kt`,
  test files, `native/operations.txt`.

### T6 — GuestChatQuery

- `dto/update.rs`: `Update::GuestChatQuery` projects as `kind: "guestChatQuery"` with a
  `GuestChatQueryDto` mirroring grammers' accessors (`grammers-client/src/update/guestchat_query.rs`:
  user, chat, title, message — the agent mirrors exactly what 0.10.0 exposes).
- New operation `answerGuestChatQuery {peer, messageId, emojiId?}` invoking
  `messages.GetBotChatAnswer` and projecting the `messages.BotChatAnswer` response. The agent
  verifies the layer semantics from the schema comments before fixing the payload.
- Kotlin: `protocol/Update.kt` + facade update model + `UpdatesApi`/`RawApi`-adjacent method.
- Owned files: `native/src/dto/update.rs`, `native/src/ops/updates.rs` (or `ops/guestchat.rs` if
  it outgrows), `protocol/Update.kt`, `UpdatesApi.kt`, test files, `native/operations.txt`.

### T7 — Inline: send result, page projection, media answers

One agent, one branch, two milestones (the files overlap):

- M1 — `sendInlineBotResult {peer, queryId, resultId, silent?, background?, clearDraft?,
  hideVia?, replyToMessageId?, scheduleDate?}` → `messages.SendInlineBotResult`. The response is
  an `Updates` bundle; extract and return the sent `MessageDto` when identifiable, else
  `{ok: true}` (the update stream delivers it — document which). Enrich `InlineQueryResultsDto`
  with `switchPm {text, startParam}`/`switchWebview {text, url}` and each result with the text its
  `send_message` would post.
- M2 — `answerInlineQuery` accepts kind-tagged results beyond articles: `photo`, `gif`, `video`,
  `voice`, `document`, each by content URL with optional thumbnail, built from the raw
  `InputBotInlineResult*`/`InputBotInlineMessage*` constructors (grammers' builder only has
  articles — the existing raw-TL precedent in this module applies). Validation mirrors the layer:
  required dimensions/mime per kind.
- Owned files: `native/src/ops/inline.rs`, `native/src/dto/inline.rs`, `protocol/Inline.kt`,
  `InlineApi.kt`, test files, `native/operations.txt`.

### T8 — Curated raw families (four sub-branches, each its own agent)

Typed operations built on grammers' TL layer — the `acceptInviteLink` precedent: build
`tl::functions::*`, invoke through the client, project the result. Each family is one new Kotlin
`*Api.kt` plus one supertype on `TelegramClient`, and lives in a new `native/src/ops/<family>.rs`.

- **T8a Contacts** (`ContactsApi`): `contactsGetContacts(hash)`, `contactsImportContacts`,
  `contactsDeleteContacts`, `contactsBlock`, `contactsUnblock`, `contactsGetBlocked`,
  `contactsSearch`. Peer results reuse `peer_dto` so handles register and peers stay sendable.
- **T8b Account** (`AccountApi`): `accountUpdateProfile`, `accountUpdateUsername`,
  `accountCheckUsername`, `accountUpdateStatus(offline)`, `accountGetAuthorizations`,
  `accountResetAuthorization`, `accountResetAuthorizations`, `accountGetPassword` (project
  hasPassword/hint/recovery/email state), `accountGetPrivacy`/`accountSetPrivacy` for the curated
  keys (status timestamp, chat invite, phone number), and per-peer
  `accountGetNotifySettings`/`accountSetNotifySettings` (mute/sound/preview).
- **T8c Dialog filters** (`FoldersApi`): `messagesGetDialogFilters`, `messagesUpdateDialogFilter`
  (create/update; deletion is an update with an empty filter — document the shape), projecting
  title, flags and pinned/included/excluded peers.
- **T8d Stickers** (`StickersApi`): `messagesGetStickerSet`, `messagesGetAllStickers`,
  `messagesGetRecentStickers`, `messagesGetFavedStickers`. Get-only; install/archive stay behind
  `invokeRaw`.
- Shared DTOs (blocked peers, authorizations, privacy rules, dialog filters, sticker sets) go in
  new `native/src/dto/<family>.rs` files and `protocol/<Family>.kt`.
- Tests: TL-fixture projection tests per DTO; Kotlin mapping and facade-delegation tests.

### T9 — Documentation and final integration

- `docs/compatibility.md`: new mapping rows for every operation above; rewrite the "Not mapped
  yet" section (rights, markup-on-send and edit parity stop being gaps; contacts/account/folders/
  stickers leave `invokeRaw`); the "grammers 0.8.1" wording is already fixed by T0.
- `README.md`: status section reflects the new coverage; gaps paragraph shrinks to what remains
  (entity projection/rendering, the code generator, background callback loop).
- `CHANGELOG.md`: one Unreleased entry per task, matching the squash commit body.
- `docs/grammers-parity-plan.md`: progress table marked complete with a pointer here.
- Final gate: regenerate `native/operations.txt`, full verification suite on the integration
  branch (`cargo build`, `cargo test`, `cargo build --release`, Gradle `clean test`), confirm the
  seven JNI exports via dumpbin, then one squashed commit to `main`, pushed.

## Waves

Branches fork from the integration branch **after T0 merges**. Agents work in `.worktrees/<task>`
and commit on `feat/<task>`; the orchestrator merges.

| Wave | Tasks in parallel | Why |
| --- | --- | --- |
| 0 | T0 | Everything forks from the pinned dependency. |
| 1 | T1, T2, T3, T6 | Disjoint files: chats/permissions, message/dialog DTOs, markup/send payloads, updates. |
| 2 | T4, T5, T7, T8a, T8b | T4/T5 build on merged T3; T7 owns inline alone; T8 families are new files. T4 and T5 both touch `ops/messages.rs`/`ops/media.rs` — T5 rebases onto integration once T4 merges, conflicts are payload-struct lines only. |
| 3 | T8c, T8d | Same new-file pattern as T8a/T8b; staggered to keep the integration branch reviewable. |
| 4 | T9 | Serial, after everything merges. |

Known merge points, all textual and one-line-class: `native/operations.txt` (regenerate with
`tools/generate_operations.py` after each merge instead of hand-merging), `native/src/ops/mod.rs`
module list, `InternalTelegramClient` bridge methods, the `TelegramClient` supertype list.

## Exit criteria

Every gap in the 2026-09-30 review is closed: full rights round-trip, enriched message projection,
markup attachable everywhere, full edit parity, stream/bytes uploads with send-by-handle,
GuestChatQuery, inline send + media answers, and the four curated raw families — with both test
suites, the operation drift gate and the JNI export check green, delivered as one squashed commit
on `main`.

## Delivery record

Landed on integration branch `feat/gap-closure`, delivered as one squashed commit to `main`.

| task | branch | state |
| --- | --- | --- |
| T0 | `feat/t0-pin-grammers-rev` | merged |
| T1 | `feat/t1-rights-roundtrip` | merged |
| T2 | `feat/t2-message-projection` | merged |
| T3 | `feat/t3-markup-attach` | merged |
| T4 | `feat/t4-edit-parity` | merged |
| T5 | `feat/t5-uploads` | merged |
| T6 | `feat/t6-guestchat-query` | merged |
| T7 | `feat/t7-inline` | merged |
| T8a | `feat/t8a-contacts` | merged |
| T8b | `feat/t8b-account` | merged |
| T8c | `feat/t8c-folders` | merged |
| T8d | `feat/t8d-stickers` | merged |
| T9 | this documentation pass | done |

Deviations from the plan, each recorded in the relevant code:

- T6 answers a guest-chat query with `messages.SetBotGuestChatResult`; the planned
  `messages.GetBotChatAnswer` does not exist at this layer.
- T8b uses `auth.ResetAuthorizations`; `account.resetAuthorizations` does not exist at this layer.
- T8c names the reorder operation after its constructor, `messages.UpdateDialogFiltersOrder`.
- T5 stores `{ name, data }` per in-progress stream, because `uploadStreamFinish` receives only the
  id while `upload_stream` needs the name.
- T3's album payloads carry no markup: `messages.SendMultiMedia` has none.
- T4 adds the entity input spec for send and edit; projecting entities on received messages and
  rendering markdown/html stay the documented follow-up (see `compatibility.md`).

Final state: 96 operations, 240 Rust tests, 249 JVM tests, the 7 frozen JNI exports verified.
