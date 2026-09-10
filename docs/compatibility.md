# Kotlogram compatibility

The public facade deliberately uses Kotlogram's package namespace:

```kotlin
import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApp
```

The bridge uses grammers' Telegram TL layer 216 schema. The following operations are mapped to
its high-level API rather than old Layer-66 TL requests:

| Kotlogram-shaped method | grammers operation |
| --- | --- |
| `authSendCode` / `authSignIn` / `authCheckPassword` | `request_login_code` / `sign_in` / `check_password` |
| `authImportBotAuthorization` | `bot_sign_in` |
| `contactsResolveUsername` | `resolve_username` |
| `messagesSendMessage` | `send_message` |
| `messagesEditMessage` | `edit_message` |
| `messagesDeleteMessages` | `delete_messages` |
| `messagesGetHistory` | `iter_messages` |
| `messagesGetMessages` / `messagesSearch` / `messagesForwardMessages` | `get_messages_by_id` / `search_messages` / `forward_messages` |
| `messagesGetPinnedMessage` / `messagesPinMessage` / `messagesUnpinMessage` | `get_pinned_message` / `pin_message` / `unpin_message` |
| `messagesSendReaction` / `messagesRemoveReaction` | `send_reactions` |
| `messagesGetDialogs` | `iter_dialogs` |
| `messagesReadHistory` | `mark_as_read` |
| `channelsJoinChannel` / `channelsLeaveChannel` | `join_chat` / `delete_dialog` |
| `channelsGetParticipants` / `channelsKickParticipant` | `iter_participants` / `kick_participant` |

`TelegramApiStorage` now provides a SQLite session path because grammers stores the auth key,
datacenter data and peer cache as one atomic session database. The old per-field storage contract
cannot safely be retained.

The generated `com.github.badoualy.telegram.tl.*` Layer-66 model is intentionally not shipped.
Using it against the supported Layer-216 schema would silently serialize stale constructors. Add
missing capabilities through a versioned raw API that uses the grammers layer instead.
