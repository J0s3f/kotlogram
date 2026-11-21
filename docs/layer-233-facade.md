# Layer 233 facade: design

Scope: give the layer-233 additions that arrived with grammers 0.12.0 a typed Kotlin facade instead of
leaving them behind `invokeRaw`. grammers has no typed helpers for any of them, so every operation here
is a native handler that builds the layer's own request (`client.invoke(&tl::functions::...)`), exactly
like `users.rs` does for `users.getUsers`.

Method: **tests first**. The wire contracts below are fixed by Kotlin protocol tests and Rust payload/DTO
tests written before any handler exists; the handlers and models are then written to make them pass.

## In scope (10 operations, 4 updates, 1 inline result kind)

### Forum sidebar (chats domain)

| Operation | Layer request | Payload | Result |
|---|---|---|---|
| `toggleForum` | `channels.toggleForum` | `channel` (peer handle), `enabled`, `tabs = false`, `sidebar = false` | `OperationResult` |

The layer changed the constructor id of `channels.toggleForum` to add `sidebar:Bool`, so the older raw
call no longer works; the facade makes `sidebar` an explicit, defaulted argument.

### Audio transcription (files domain)

| Operation | Layer request | Payload | Result |
|---|---|---|---|
| `transcribeAudioFile` | `messages.transcribeAudioFile` | `uploadHandle` (a handle from any `upload*` operation) | `AudioTranscription(transcriptionId, text, pending, trialRemaining?, trialRemainingUntilDate?)` |
| `getTranscriptionLangs` | `messages.getTranscriptionLangs` | `peer?` (peer handle) | `List<String>` of language codes |

`pending = true` means `text` is partial; the final text arrives as an `audioTranscribed` update with
the same `transcriptionId`.

### Widgets (new `widgets` domain)

A widget is an HTML payload a bot attaches to a message. Public data is readable by everyone who sees the
message, chat data per chat. Wallet-carrying query types (`widgetQueryTypeSignData`,
`widgetQueryTypeSentTransaction`) and `wallet.card` are **out of scope**: they stay behind `invokeRaw`.

| Operation | Layer request | Payload | Result |
|---|---|---|---|
| `sendWidget` | `messages.sendMedia` with `inputMediaWidget` / `inputMediaWidgetId` | `peer`, `widget` (`WidgetSpec`) or `widgetId` + `chatData?`, `caption`, `replyToMessageId?`, `silent`, `scheduleDate?` | `SentWidget(ok, message?)` |
| `getWidgetPublicData` | `messages.getWidgetPublicData` | `peer`, `messageId`, `hash = 0` | `WidgetPublicData(notModified, hash?, data?, periodSeconds)` |
| `editMessageWidgetData` | `messages.editMessageWidgetData` | `peer`, `messageId`, `publicData?`, `chatData?` | `OperationResult` |
| `editInlineMessageWidgetData` | `messages.editInlineBotMessageWidgetData` | inline message id, `publicData?`, `chatData?` | `{edited}` |
| `editWidgetData` | `messages.editWidgetData` | `widgetId`, `publicData`, `cacheTimeSeconds?` | `OperationResult` |
| `getBotWidgetQueryAnswer` | `messages.getBotWidgetQueryAnswer` | `peer`, `messageId`, `dataBase64`, `withWallet = false` | `{result?}` |
| `setBotWidgetQueryAnswer` | `messages.setBotWidgetQueryAnswer` | `queryId`, `result?` | `OperationResult` |

`WidgetSpec(html, width, height, allowQueries = false, sharedWidget = false, publicData?, chatData?)`.
`width`/`height` must be positive and `html` non-empty; the handler rejects violations before any request.

`InlineResultSpec` gains `kind = "widget"` (plus `widget: WidgetSpec?`, `widgetId: Long?`), built as an
`inputBotInlineResult` whose send message is `inputBotInlineMessageWidget` / `...WidgetId`.

### Updates

The update stream types four more events, projected from the raw update grammers delivers (the variant
stays `raw`-delivered by grammers, so the bridge names it itself):

| `kind` | Field | Fields |
|---|---|---|
| `forumTopicEdited` | `forumTopicEdit` | `peer` (id + kind), `topicId`, `title?`, `iconEmojiId?`, `closed?`, `hidden?` |
| `botWidgetQuery` | `widgetQuery` | `queryId`, `userId`, `peer?` + `messageId?` **or** `inlineMessageId?`, `chatInstance`, `chatType`, `dataBase64`, `widgetId?`, `queryType` (`data` / `signData` / `sentTransaction`) |
| `messageWidgetData` | `widgetData` | `peer`, `messageId`, `publicData?`, `publicDataHash?`, `chatData?` |
| `audioTranscribed` | `transcription` | `transcriptionId`, `text`, `pending` |

`rawUpdate` stays populated for these kinds so no information is lost.

## Layering (same as every other domain)

1. `native/src/ops/<domain>.rs`: `OPERATIONS`, `route`, handlers, payload structs, request builders as
   pure functions (that is what the Rust unit tests pin).
2. `native/src/dto/*.rs`: result and update projections.
3. `org.kotlogramme.protocol.*`: serializable payloads/results (the wire contract).
4. `org.kotlogramme.bridge.*Bridge` (`@Operation`): the typed call, registered in `Operation.kt` and
   `TelegramClient.kt`.
5. `com.github.badoualy.telegram.api.*Api`: the Kotlogram-style interface and model mapping.
6. `native/operations.txt` via `tools/generate_operations.py` (parity-tested on both sides).

## Test plan (written first)

- Kotlin `protocol/Layer233ProtocolTest`: payload encodings (exact JSON), result and update decoding,
  defaults, the `widget` inline kind.
- Rust, per handler module: payload decode, request builders produce the exact `tl::functions` values
  (flags derived correctly, `sidebar` threaded, widget vs widget-id media), validation errors, routing.
- Rust `dto/update.rs`: each of the four raw updates projects to the typed field and keeps `rawUpdate`;
  an unrelated raw update is unchanged.
- Parity gates (`OperationParityTest`, Rust `ops/tests.rs`) cover the inventory.
- No live-Telegram test: like every other family, the handlers that need a session are covered through
  their pure builders and projections.

## Out of scope / follow-ups

Wallet query types and cards, `messageMediaWidget` projection on received messages (needs a `Media`
variant), `forum_sidebar` projection on projected chats, and the legacy `Kotlogram` compatibility
mapping for the new updates beyond `TypedUpdate`.
