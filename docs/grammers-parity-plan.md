# grammers parity plan

Goal: make every capability of the Rust implementation reachable, and faithfully modelled, from
Kotlin. The "Rust implementation" is the native JNI crate in `native/`, whose functional surface is
the grammers 0.8.1 `Client` API it wraps. Kotlin currently covers 26 of those operations and
projects them through lossy DTOs.

## Current coverage

All ten Phase 1 domains are merged on `feat/grammers-parity`. The native crate routes 66
operations, listed in `native/operations.txt` (verified equal to the dispatcher by the Rust tests
and to the annotated Kotlin bridge by `OperationParityTest`), and `docs/compatibility.md` maps the
Kotlogram-shaped facade surface to the grammers calls behind it. The seven JNI exports
(`create`, `close`, `invokeRaw`, `isAuthorized`, `signInBot`, `sendMessage`, `request`) are
unchanged.

## Gaps

### Model fidelity (grammers accessors not projected)

| grammers 0.8.1 source | missing projection |
| --- | --- |
| `types/peer/user.rs` | phone, bot/verified/scam/restricted/support flags, `status`, lang, contact flags, restriction reasons, alternate usernames |
| `types/peer/{group,channel}.rs` | title, alternate usernames, photo, megagroup flag, admin rights |
| `types/message.rs` | date, edit date, media, sender, peer, view/forward/reply/reaction counts, pinned, mentioned, silent, post, `grouped_id`, `via_bot_id`, post author, reply markup, entities, forward header, action, `html_text`/`markdown_text` |
| `types/media.rs`, `types/photo_sizes.rs` | all `Media` variants (photo, document, sticker, contact, poll, geo, dice, venue, geo-live, web page) with sizes, duration, resolution, spoiler, ttl |
| `types/dialog.rs` | unread count, pinned, top message, draft, folder (readable from `Dialog.raw`) |
| `types/participant.rs`, `types/permissions.rs` | role detail, date, rank, permissions, restrictions, inviter |

### Missing operations

| grammers 0.8.1 API | source |
| --- | --- |
| `sign_out` | `client/auth.rs` |
| `iter_messages` paging (`offset_id`, `min_date`, `max_date`), `total()` | `client/messages.rs` |
| `search_messages` options (`sent_by_self`, dates, `filter`), `search_all_messages`, `total()` | `client/messages.rs` |
| `get_reply_to_message` | `client/messages.rs` |
| `InputMessage` extras: html/markdown/entities, reply markup, `invert_media`, `schedule_date`, `mime_type`, `media_ttl`, typed `media`, `copy_media` | `types/input_message.rs` |
| `InputMedia` extras: thumb, url, attribute, mime, ttl, html/markdown caption | `types/input_media.rs` |
| `download_media`, `iter_download` (`chunk_size`, `skip_chunks`), `upload_stream`, upload metadata | `client/files.rs` |
| `iter_profile_photos` | `client/chats.rs` |
| `get_permissions`, `set_banned_rights`, `set_admin_rights` | `client/chats.rs` |
| `accept_invite_link`, `parse_invite_link` | `client/chats.rs` |
| `resolve_peer` by id | `client/chats.rs` |
| participant filters (admins, bots, banned, contacts, search, kicked), `total()` | `client/chats.rs` |
| `clear_mentions` | `client/dialogs.rs` |
| typed `Update` payloads, `next_raw`, `sync_update_state` | `client/updates.rs`, `types/update/*` |
| `ActionSender` / `SendMessageAction` | `types/action.rs` |
| `Button` / `ReplyMarkup` builders | `types/button.rs`, `types/reply_markup.rs` |
| `InlineBot`, `inline_query`, `edit_inline_message` | `client/bots.rs` |
| chat photos via `iter_messages(peer).filter(InputMessagesFilterChatPhotos)` | `client/messages.rs` |

grammers 0.8.1 exposes no typed contacts or account API, so those families stay behind
`RawTelegramApi` / `invokeRaw` unless a typed raw-wrapper layer is added.

## Delivery shape

The bridge is currently one 982-line `native/src/lib.rs` with a single `match` over operation
names, and the Kotlin bridge keeps every payload class in one file. Feature branches touching
those files cannot be merged independently. The plan therefore lands a foundation first that gives
each domain its own files, after which domains are genuinely independent.

### Phase 0 (serial)

- **0A structural split** — `native/src/{lib,client,error}.rs` plus `native/src/dto/*` and
  `native/src/ops/*`; each op module exports `OPERATIONS: &[&str]` and a
  `route(operation) -> Option<Handler>`. Kotlin payloads move to
  `org/kotlogramme/protocol/*`, public models to `com/github/badoualy/telegram/api/*`, and the
  facade becomes `interface TelegramClient : <domain interfaces>` with default implementations
  delegating to the bridge. No behaviour change.
- **0B drift gate** — `@Operation("name")` on every bridge method; `native/operations.txt` is the
  shared inventory, asserted equal to `all_operations()` plus the JNI names by a Rust unit test
  and equal to the annotated Kotlin methods by `OperationParityTest`. Adding an operation on one
  side without the other fails the build.
- **0C rich entity model** — full grammers-backed DTOs in `native/src/dto/*` and their Kotlin
  mirrors, with wire-contract tests on both sides. Still no new operations.
- **0D bridge split** — the bridge's operations move out of the one `TelegramClient` class into one
  interface per domain under `org/kotlogramme/bridge/*`, over a shared `Transport`. Every domain
  is pre-declared on the client and in `OperationCatalog`, so a feature task edits only its own
  file. No behaviour change.

After 0A–0D every file a feature task touches is owned by exactly one task, so the Phase 1 branches
merge without conflict. `native/operations.txt` is the single shared file, and
`tools/generate_operations.py` re-derives it from the dispatcher, so a merge conflict there is
resolved by re-running the script.

### Phase 1 (parallel, one worktree and branch per task)

| task | owns | scope |
| --- | --- | --- |
| `auth` | `ops/auth.rs`, `bridge/AuthBridge.kt`, `api/AuthApi.kt` | `signOut`, full `getMe`, data-centre id |
| `messages` | `ops/messages.rs`, `bridge/MessagesBridge.kt`, `api/MessagesApi.kt` | history paging and totals, search options, global search, reply resolution, chat photos |
| `media` | `ops/media.rs`, `bridge/MediaBridge.kt`, `api/MediaApi.kt` | typed media send, captions, html/markdown, spoiler, mime, ttl, copy media |
| `markup` | `ops/markup.rs`, `bridge/MarkupBridge.kt`, `api/MarkupApi.kt` | inline, reply, force-reply and hide markup; all button kinds |
| `files` | `ops/files.rs`, `bridge/FilesBridge.kt`, `api/FilesApi.kt` | `downloadMedia`, chunk ranges, upload metadata, profile photos |
| `chats` | `ops/chats.rs`, `bridge/ChatsBridge.kt`, `api/ChatsApi.kt` | permissions, ban and admin rights, invite links, resolve by id, participant filters |
| `dialogs` | `ops/dialogs.rs`, `bridge/DialogsBridge.kt`, `api/DialogsApi.kt` | `clearMentions`, dialog metadata, totals |
| `updates` | `ops/updates.rs`, `bridge/UpdatesBridge.kt`, `api/UpdatesApi.kt` | typed updates, raw update, state sync, `UpdateCallback` dispatch |
| `inline` | `ops/inline.rs`, `bridge/InlineBridge.kt`, `api/InlineApi.kt` | inline bot identity, answer, next result, edit |
| `actions` | `ops/actions.rs`, `bridge/ActionsBridge.kt`, `api/ActionsApi.kt` | chat actions |

`ops/raw.rs` and `RawApi` stay placeholders: grammers 0.8.1 exposes no typed contacts or account
API, so those families stay behind `RawTelegramApi` / `invokeRaw`.

Each task also adds its new payload and result classes to the `protocol/<domain>.kt` file for its
domain, and its own projection types to `dto/<domain>.rs` where one is needed, registered with a
single `mod` line in `native/src/dto/mod.rs`.

### Phase 2 (serial)

Compatibility table and README refresh, live integration test extension, release notes, final
review.

## Verification

Baseline verified on this machine:

```
cargo build --manifest-path native\Cargo.toml
$env:JAVA_HOME="C:\Users\micro\.jdks\liberica-21.0.6"
.\gradlew.bat --no-daemon "-Porg.gradle.java.installations.paths=C:\Users\micro\.jdks\liberica-17.0.15,C:\Users\micro\.jdks\liberica-21.0.6" test
```

JDK 25 is the default `JAVA_HOME` but Gradle 8.10 rejects it, and the Adoptium 17 install under
`C:\Program Files\Eclipse Adoptium` is broken (no `bin`). Liberica 21 as the launcher with
Liberica 17 registered as a toolchain works. Every task must pass `cargo build`, `cargo test` and
Gradle `test` in its own worktree.

## Merge procedure

1. Create `git worktree add .worktrees/<domain> -b feat/parity-<domain> feat/grammers-parity`.
2. Agent commits in its worktree only.
3. Review the diff on the branch, then integrate into `feat/grammers-parity` in the main worktree
   with `git merge --no-ff`. The only expected conflict is `native/operations.txt`; resolve it by
   taking either side and re-running `python tools/generate_operations.py`, then confirm
   `git diff native/operations.txt` is empty against the merged dispatcher.
4. Re-run the full verification on the integration branch after each merge.

## Progress

Integration branch `feat/grammers-parity`. Phase 0 (foundation, models, bridge split) and Phase 1
per-domain branches merge with `--no-ff`; `native/operations.txt` is regenerated after any conflict.

| phase | scope | state |
| --- | --- | --- |
| 0A–0D | structural split, drift gate, rich models, bridge split, inventory script | merged |
| 1 `actions` | chat actions and the service action of a message | merged |
| 1 `updates` | typed update payloads, the raw update and the update state | merged |
| 1 `markup` | the four reply markups and every button kind | merged |
| 1 `auth` | `signOut`, full `getMe`, data-centre id | merged |
| 1 `messages` | history paging and totals, search options, global search, reply resolution, chat photos | merged |
| 1 `media` | typed media send, captions, html/markdown, spoiler, mime, ttl, copy media | merged |
| 1 `files` | `downloadMedia`, chunk ranges, upload metadata, profile photos | merged |
| 1 `chats` | permissions, ban and admin rights, invite links, resolve by id, participant filters | merged |
| 1 `dialogs` | `clearMentions`, dialog metadata, totals | merged |
| 1 `inline` | inline bot identity, answer, next result, edit | merged |
| — | review fixes: `Transport.decode` accepts a bare `null`; invite-link host compared case-insensitively | merged |
| 2 | compatibility table, README refresh, live-test extension, release notes, final review | done: `docs/compatibility.md` maps the whole facade, README and plan refreshed, `LiveTelegramIntegrationTest` gained a media/file scenario, `CHANGELOG.md` written, final review passed |

Test counts on the integration branch after each merge (Rust unit tests / Kotlin tests): 16 / 28 at
the Phase 0 baseline, 35 / 56 after `updates`, 59 / 80 after `markup`, 65 / 89 after `auth`,
76 / 99 after `media`, 87 / 114 after `files`, 92 / 123 after `dialogs`, 100 / 134 after
`messages`, 118 / 146 after `chats`, and **132 / 158 with all ten domains merged** (after a final
`cargo fmt` pass). The 7 frozen JNI export names were dumped and verified against the release DLL
after the final merge.

The post-parity gap-closure work is recorded in
[`gap-closure-roadmap.md`](gap-closure-roadmap.md): it closed the rights round-trip, markup-on-send,
full edit parity, message-projection, guest-chat, inline, upload and typed-raw-family gaps, bringing
the surface to **96 operations / 240 Rust tests / 249 Kotlin tests**.

