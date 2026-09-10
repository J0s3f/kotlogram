# Kotlogram compatibility

The public facade deliberately uses Kotlogram's package namespace:

```kotlin
import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApp
```

The following operations are mapped to grammers' high-level API rather than old Layer-66 TL
requests:

| Kotlogram-shaped method | grammers operation |
| --- | --- |
| `authSendCode` / `authSignIn` / `authCheckPassword` | `request_login_code` / `sign_in` / `check_password` |
| `authImportBotAuthorization` | `bot_sign_in` |
| `contactsResolveUsername` | `resolve_username` |
| `messagesSendMessage` | `send_message` |
| `messagesEditMessage` | `edit_message` |
| `messagesDeleteMessages` | `delete_messages` |
| `messagesGetHistory` | `iter_messages` |
| `messagesGetDialogs` | `iter_dialogs` |
| `messagesReadHistory` | `mark_as_read` |
| `channelsJoinChannel` / `channelsLeaveChannel` | `join_chat` / `delete_dialog` |

`TelegramApiStorage` now provides a SQLite session path because grammers stores the auth key,
datacenter data and peer cache as one atomic session database. The old per-field storage contract
cannot safely be retained.

The generated `com.github.badoualy.telegram.tl.*` Layer-66 model is intentionally not shipped.
Using it against a current Telegram layer would silently serialize stale constructors. Add missing
capabilities through a versioned raw API that uses the grammers layer instead.
