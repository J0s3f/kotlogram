# Kotlogram compatibility

The public facade deliberately uses Kotlogram's package namespace:

```kotlin
import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApp
```

The bridge uses grammers' Telegram TL layer 216 schema. The following operations are mapped to
its high-level API rather than the old Layer-66 TL requests Kotlogram generated:

| Kotlogram-shaped method | grammers operation |
| --- | --- |
| `authSendCode` / `authSignIn` / `authCheckPassword` | `request_login_code` / `sign_in` / `check_password` |
| `authImportBotAuthorization` | `bot_sign_in` |
| `authLogOut` | `sign_out` |
| `getMe` / `getAccountIdentity` / `getDataCentreId` | `get_me` + `Session::home_dc_id` |
| `contactsResolveUsername` | `resolve_username` |
| `messagesSendMessage` | `send_message` |
| `messagesSendFile` | `upload_file` + `send_message` |
| `messagesSendAlbum` | `upload_file` + `send_album` |
| `messagesEditMessage` | `edit_message` |
| `messagesDeleteMessages` | `delete_messages` |
| `messagesGetHistory` | `iter_messages` |
| `messagesGetHistoryTotal` | `MessageIter::total` |
| `messagesGetChatPhotos` | `search_messages(...).filter(InputMessagesFilterChatPhotos)` |
| `messagesGetReplyToMessage` | `get_reply_to_message` |
| `messagesGetMessages` / `messagesForwardMessages` | `get_messages_by_id` / `forward_messages` |
| `messagesSearch` / `messagesSearchTotal` | `search_messages` (+ options, `.total()`) |
| `messagesSearchGlobal` / `messagesSearchGlobalTotal` | `search_all_messages` (+ `.total()`) |
| `messagesGetPinnedMessage` / `messagesPinMessage` / `messagesUnpinMessage` | `get_pinned_message` / `pin_message` / `unpin_message` |
| `messagesSendReaction` / `messagesRemoveReaction` | `send_reactions` |
| `messagesGetDialogs` / `messagesGetDialogsMeta` | `iter_dialogs` (+ raw-layer state off `Dialog::raw`) |
| `messagesGetDialogsTotal` | `DialogIter::total` |
| `messagesReadHistory` / `messagesClearMentions` | `mark_as_read` / `clear_mentions` |
| `channelsJoinChannel` / `channelsLeaveChannel` | `join_chat` / `delete_dialog` |
| `channelsGetParticipants` / `channelsKickParticipant` | `iter_participants` / `kick_participant` |
| `channelsGetParticipantPermissions` | `get_permissions` |
| `channelsEditBanned` / `channelsEditAdmin` | `set_banned_rights` / `set_admin_rights` |
| `messagesImportChatInvite` / `messagesParseInviteLink` | `accept_invite_link` / `parse_invite_link` (rebuilt on the same requests, see below) |
| `channelsResolvePeer` | `resolve_peer` by Bot API dialog id |
| `mediaSend` / `mediaSendUrl` / `mediaCopy` | `upload_file`/URL + `InputMessage::{photo,document,file,photo_url,document_url,copy_media}` with `html`/`markdown` captions, `media_ttl`, `mime_type`, spoiler and scheduling |
| `downloadMedia` / `downloadMediaChunk` / `uploadFile` | `download_media` / `iter_download` chunk paging / `upload_file` |
| `getProfilePhotos` | `iter_profile_photos` |
| `getNextUpdate` / `getNextTypedUpdate` / `getNextRawUpdate` | `UpdateStream::next` / `next_raw` (typed projection and the raw TL update) |
| `syncUpdateState` | `UpdateStream::sync_update_state` |
| `inlineQuery` | `messages.GetInlineBotResults` (`Client::inline_query`, keeping the query id and next offset) |
| `answerCallbackQuery` | `messages.SetBotCallbackAnswer` (the request behind `CallbackQuery::answer`) |
| `answerInlineQuery` | `messages.SetInlineBotResults` (the request behind `InlineQuery::answer`) |
| `editInlineMessage` | `edit_inline_message` (the send behind `InlineSend::edit_message`) |
| `markupBuildInline` / `markupBuildKeyboard` / `markupBuildForceReply` / `markupBuildHide` | `reply_markup::{inline,keyboard,force_reply,hide}` + `button::*` |
| `markupGetReplyMarkup` | `Message::reply_markup` |
| `actionsSendChatAction` / `actionsCancelChatAction` | `ActionSender` / `SendMessageAction` |
| `actionsGetMessageAction` | `Message::action` (projected name/message/sender) |

Two of those rows deserve a note. `accept_invite_link` and `parse_invite_link` are behind a grammers
optional feature this crate does not enable, so the bridge rebuilds the same surface: the
`messages.ImportChatInvite` request for the invite and a URL parser that follows grammers' own
host and path rules (verified against its source). Similarly, grammers' `Client::edit_inline_message`
is crate-private, so editing an inline message invokes the `messages.EditInlineBotMessage` request
directly. Date-bounded peer search, on the other hand, is back on grammers' own `SearchIter`: its
date bounds take a `jiff::Timestamp`, which the bridge carries `jiff` for, so no raw request is
needed there.

`TelegramApiStorage` now provides a SQLite session path because grammers stores the auth key,
datacenter data and peer cache as one atomic session database. The old per-field storage contract
cannot safely be retained.

The generated `com.github.badoualy.telegram.tl.*` Layer-66 model is intentionally not shipped.
Using it against the supported Layer-216 schema would silently serialize stale constructors. Add
missing capabilities through a versioned raw API that uses the grammers layer instead.

## Facade shape

`TelegramClient` is composed of one interface per domain — `AuthApi`, `MessagesApi`, `ChatsApi`,
`DialogsApi`, `UpdatesApi`, `MediaApi`, `FilesApi`, `InlineApi`, `ActionsApi`, `MarkupApi` and
`RawApi` — each declaring its methods with default implementations that delegate to the grammers
bridge. `RawApi` stays a placeholder: grammers 0.8.1 exposes no typed contacts or account API, so
those families stay behind `RawTelegramApi`. A new capability is one `*Api.kt` file plus one
supertype, so domains can be developed and reviewed independently.

`native/operations.txt` is the shared contract listing every operation the native crate answers.
The Rust unit tests and `OperationParityTest` on the JVM both assert against it, so the bridge
cannot declare a capability the native side does not implement, or the reverse. It currently
lists 66 operations. [`docs/grammers-parity-plan.md`](grammers-parity-plan.md) records the gap
analysis and how each domain was closed.

## Raw API contract

`org.kotlogramme.raw.RawTelegramApi` bundles a generated `kotlogram-raw-schema/v1` manifest for
Layer 216. Its experimental `invoke` method transmits an already TL-encoded request through the
grammers sender pool and returns the raw response bytes. The caller must use a codec generated for
the same manifest version and layer.

The manifest generator is deliberately separate from the JVM build. CI regenerates it from the
schema resolved by Cargo and compares it with the committed resource. This is the deployment gate
for a future code generator that produces Kotlin codecs and Rust method dispatchers; a grammers
upgrade must create a new `telegram-tl-<layer>` API version instead of mutating an existing one.

## Not mapped yet

grammers 0.8.1 exposes no typed contacts or account API, so `account.*` (password settings,
username changes, authorizations, notifications) stays behind `invokeRaw`. The legacy
`UpdateCallback` is delivered on demand by `UpdatesApi.dispatchNextUpdate` rather than by a
background loop. Everything in `native/operations.txt` is reachable from Kotlin; anything beyond
it is a grammers capability this layer has not added yet.