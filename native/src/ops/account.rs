//! Typed account operations built on grammers' TL layer.
//!
//! grammers exposes no account-management surface of its own, so this module follows the
//! `acceptInviteLink` precedent in [`super::chats`]: each operation builds one
//! `tl::functions::account::*` request, invokes it through the client and projects the result
//! through [`crate::dto::account`]. Every request here is a plain method with no grammers-side
//! builder to drift from, so the payload shapes are the whole contract.
//!
//! Privacy is curated deliberately: `account.GetPrivacy`/`account.SetPrivacy` accept only the
//! `statusTimestamp`, `chatInvite` and `phoneNumber` keys. The layer knows more keys, and more
//! rule kinds, but those stay behind `invokeRaw` until a caller needs them. Setting a rule that
//! names users (`allowUsers`/`disallowUsers`) is refused: the layer takes `InputUser` values,
//! which need each user's access hash, and this operation takes bare user ids nowhere.
//!
//! Notification settings are scoped by name, because the layer's `inputNotifyPeer` has no
//! "the whole account" constructor: [`NotifyScope::Account`] is the bridge's own scope and is sent
//! as the logged-in user's `inputPeerSelf`, which is how the layer spells the account-wide
//! settings. Making the scope explicit keeps an account-wide answer distinguishable from a
//! peer-specific one, which the layer's answer cannot say on its own.

use grammers_client::peer::User as ClientUser;
use grammers_client::tl;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::account::{authorizations_dto, password_settings_dto, privacy_rules_dto};
use crate::dto::notifications::notify_settings_dto;
use crate::dto::user_dto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// The privacy keys `accountGetPrivacy`/`accountSetPrivacy` accept, as a caller names them.
const PRIVACY_KEYS: &[&str] = &["statusTimestamp", "chatInvite", "phoneNumber"];

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UpdateProfilePayload {
    /// Absent leaves the current first name unchanged.
    first_name: Option<String>,
    /// Absent leaves the current last name unchanged.
    last_name: Option<String>,
    /// Absent leaves the current bio unchanged.
    about: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UsernamePayload {
    /// Empty removes the username, which the layer accepts.
    username: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UpdateStatusPayload {
    /// True hides the account's online status instead of showing it.
    offline: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ResetAuthorizationPayload {
    /// The `hash` [`AuthorizationDto`] reports for the session to drop.
    hash: i64,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct PrivacyKeyPayload {
    key: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SetPrivacyPayload {
    key: String,
    #[serde(default)]
    rules: Vec<PrivacyRuleSpec>,
}

/// One requested privacy rule, named by the layer constructor less its `privacyValue` prefix.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct PrivacyRuleSpec {
    kind: String,
    /// The bare chat ids a `*ChatParticipants` rule carries; ignored by the other kinds.
    #[serde(default)]
    chats: Vec<i64>,
}

/// Which notifications an operation addresses, named the way the layer's `inputNotifyPeer`
/// constructors are.
///
/// [NotifyScope::Account] has no layer constructor of its own: it is the account-wide scope, sent as
/// the logged-in user's `inputPeerSelf`.
#[derive(Clone, Copy, Debug, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
enum NotifyScope {
    Account,
    Peer,
    Users,
    Chats,
    Broadcasts,
    ForumTopic,
    Community,
}

impl NotifyScope {
    /// The wire name of a scope, which is also what the answer echoes back.
    fn name(self) -> &'static str {
        match self {
            NotifyScope::Account => "account",
            NotifyScope::Peer => "peer",
            NotifyScope::Users => "users",
            NotifyScope::Chats => "chats",
            NotifyScope::Broadcasts => "broadcasts",
            NotifyScope::ForumTopic => "forumTopic",
            NotifyScope::Community => "community",
        }
    }

    /// True for the scopes that name a peer, and so need one resolved before the call.
    fn names_a_peer(self) -> bool {
        matches!(
            self,
            NotifyScope::Peer | NotifyScope::ForumTopic | NotifyScope::Community
        )
    }
}

/// Payload of `accountGetNotifySettings`: which notifications to read.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GetNotifySettingsPayload {
    scope: NotifyScope,
    /// The peer a `peer`, `forumTopic` or `community` scope addresses; the other scopes ignore it.
    #[serde(flatten)]
    peer: PeerTarget,
    /// The forum topic's top message id, which only a `forumTopic` scope reads.
    #[serde(default)]
    top_msg_id: Option<i32>,
}

/// Payload of `accountUpdateNotifySettings`: which notifications to write, and how.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UpdateNotifySettingsPayload {
    #[serde(flatten)]
    target: GetNotifySettingsPayload,
    settings: NotifySettingsSpec,
}

/// The settings `accountUpdateNotifySettings` writes, named the way `inputPeerNotifySettings` names
/// them.
///
/// Every field is optional because the layer's settings are flags: an absent one leaves whatever
/// Telegram has in place alone, which is the only way to change one setting without resetting the
/// others.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct NotifySettingsSpec {
    #[serde(default)]
    show_previews: Option<bool>,
    #[serde(default)]
    silent: Option<bool>,
    /// Epoch milliseconds at which the mute lifts; `0` unmutes now. Absent leaves the mute alone.
    #[serde(default)]
    mute_until: Option<i64>,
    #[serde(default)]
    sound: Option<NotifySoundSpec>,
    #[serde(default)]
    stories_muted: Option<bool>,
    #[serde(default)]
    stories_hide_sender: Option<bool>,
    #[serde(default)]
    stories_sound: Option<NotifySoundSpec>,
}

/// One requested notification sound, named the way `notificationSound` names its constructors.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct NotifySoundSpec {
    /// `default`, `none`, `local` or `ringtone`.
    kind: String,
    /// A ringtone's identifier; only a `ringtone` sound reads it.
    #[serde(default)]
    id: Option<i64>,
    /// A local sound's shown title; only a `local` sound reads it.
    #[serde(default)]
    title: Option<String>,
    /// A local sound's data blob; only a `local` sound reads it.
    #[serde(default)]
    data: Option<String>,
}

#[cfg_attr(not(test), allow(dead_code))]
/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "accountUpdateProfile",
    "accountUpdateUsername",
    "accountCheckUsername",
    "accountUpdateStatus",
    "accountGetAuthorizations",
    "accountResetAuthorization",
    "accountResetAuthorizations",
    "accountGetPassword",
    "accountGetPrivacy",
    "accountSetPrivacy",
    "accountGetNotifySettings",
    "accountUpdateNotifySettings",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "accountUpdateProfile" => update_profile,
        "accountUpdateUsername" => update_username,
        "accountCheckUsername" => check_username,
        "accountUpdateStatus" => update_status,
        "accountGetAuthorizations" => get_authorizations,
        "accountResetAuthorization" => reset_authorization,
        "accountResetAuthorizations" => reset_authorizations,
        "accountGetPassword" => get_password,
        "accountGetPrivacy" => get_privacy,
        "accountSetPrivacy" => set_privacy,
        "accountGetNotifySettings" => get_notify_settings,
        "accountUpdateNotifySettings" => update_notify_settings,
        _ => return None,
    })
}

/// Updates the account's first name, last name and bio, answering the updated account.
fn update_profile(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UpdateProfilePayload = parse_payload(payload)?;
    let user = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::UpdateProfile {
                    first_name: data.first_name,
                    last_name: data.last_name,
                    about: data.about,
                }),
        )
        .map_err(invocation_error)?;
    project_user(native, user)
}

/// Replaces the account's username, answering the updated account. An empty username removes it.
fn update_username(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UsernamePayload = parse_payload(payload)?;
    let user = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::UpdateUsername {
                    username: data.username,
                }),
        )
        .map_err(invocation_error)?;
    project_user(native, user)
}

/// Reports whether a username is available, which the layer answers as a bare boolean.
fn check_username(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UsernamePayload = parse_payload(payload)?;
    let available = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::CheckUsername {
                    username: data.username,
                }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "available": available }))
}

/// Reports the account online or offline, which is what a caller sets when hiding presence.
fn update_status(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UpdateStatusPayload = parse_payload(payload)?;
    native
        .runtime
        .block_on(native.client.invoke(&tl::functions::account::UpdateStatus {
            offline: data.offline,
        }))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Lists the account's active sessions.
fn get_authorizations(native: &NativeClient, _payload: &str) -> Result<String, String> {
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::GetAuthorizations {}),
        )
        .map_err(invocation_error)?;
    let tl::enums::account::Authorizations::Authorizations(authorizations) = result;
    json_string(authorizations_dto(&authorizations))
}

/// Drops one session by the hash its projection reported, signing that session out.
fn reset_authorization(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ResetAuthorizationPayload = parse_payload(payload)?;
    native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::ResetAuthorization { hash: data.hash }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Drops every session but the one this call is made from.
///
/// The pinned layer no longer carries `account.resetAuthorizations`; the method moved to
/// `auth.resetAuthorizations`, which is the same "terminate all other sessions" call. The bridge
/// keeps the Kotlogram-style `accountResetAuthorizations` name.
fn reset_authorizations(native: &NativeClient, _payload: &str) -> Result<String, String> {
    native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::auth::ResetAuthorizations {}),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Reports whether the account has a two-factor password, and what Telegram shows about it.
fn get_password(native: &NativeClient, _payload: &str) -> Result<String, String> {
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::GetPassword {}),
        )
        .map_err(invocation_error)?;
    let tl::enums::account::Password::Password(password) = result;
    json_string(password_settings_dto(&password))
}

/// Reads one curated privacy setting.
fn get_privacy(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: PrivacyKeyPayload = parse_payload(payload)?;
    let key = privacy_key_input(&data.key)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::GetPrivacy { key }),
        )
        .map_err(invocation_error)?;
    let tl::enums::account::PrivacyRules::Rules(rules) = result;
    json_string(privacy_rules_dto(&data.key, &rules))
}

/// Writes one curated privacy setting, answering the rules that are in place afterwards.
fn set_privacy(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SetPrivacyPayload = parse_payload(payload)?;
    let key = privacy_key_input(&data.key)?;
    let rules = data
        .rules
        .iter()
        .map(privacy_rule_input)
        .collect::<Result<Vec<_>, _>>()?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::SetPrivacy { key, rules }),
        )
        .map_err(invocation_error)?;
    let tl::enums::account::PrivacyRules::Rules(rules) = result;
    json_string(privacy_rules_dto(&data.key, &rules))
}

/// Reads the notification settings of one scope.
///
/// The answer carries no scope of its own on this layer, so the scope name travels with it: an
/// account-wide answer and a per-peer one are otherwise the same document.
fn get_notify_settings(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetNotifySettingsPayload = parse_payload(payload)?;
    let peer = notify_peer_input(native, &data)?;
    let result = native
        .runtime
        .block_on(native.client.invoke(&tl::functions::account::GetNotifySettings {
            peer,
        }))
        .map_err(invocation_error)?;
    json_string(notify_settings_dto(data.scope.name(), &result))
}

/// Writes the notification settings of one scope, answering the layer's bare boolean.
///
/// The write is the same call as the read for both directions: the layer's settings are flags, so an
/// absent field in [NotifySettingsSpec] is what leaves that one setting as it was.
fn update_notify_settings(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UpdateNotifySettingsPayload = parse_payload(payload)?;
    let peer = notify_peer_input(native, &data.target)?;
    let settings = tl::enums::InputPeerNotifySettings::Settings(notify_settings_input(&data.settings)?);
    native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::account::UpdateNotifySettings { peer, settings }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Projects a layer user an account update answered with.
fn project_user(native: &NativeClient, user: tl::enums::User) -> Result<String, String> {
    let user = ClientUser::from_raw(&native.client, user);
    json_string(user_dto(&user))
}

/// Builds the layer's `inputNotifyPeer` for the scope a payload names.
///
/// `account` resolves to no peer at all: the layer spells the account-wide settings as the
/// logged-in user's `inputPeerSelf`. The scopes that do not name a peer map straight onto the
/// layer's own peer-less constructors.
fn notify_peer_input(
    native: &NativeClient,
    data: &GetNotifySettingsPayload,
) -> Result<tl::enums::InputNotifyPeer, String> {
    if !data.scope.names_a_peer() {
        return Ok(match data.scope {
            NotifyScope::Account => notify_peer_self(),
            NotifyScope::Users => tl::enums::InputNotifyPeer::InputNotifyUsers,
            NotifyScope::Chats => tl::enums::InputNotifyPeer::InputNotifyChats,
            NotifyScope::Broadcasts => tl::enums::InputNotifyPeer::InputNotifyBroadcasts,
            // Guarded by `names_a_peer`.
            NotifyScope::Peer | NotifyScope::ForumTopic | NotifyScope::Community => {
                unreachable!("these scopes name a peer and are resolved below")
            }
        });
    }
    let peer = native
        .runtime
        .block_on(resolve_peer(native, &data.peer))?;
    let input = tl::enums::InputPeer::from(peer);
    Ok(match data.scope {
        NotifyScope::Peer => tl::enums::InputNotifyPeer::Peer(tl::types::InputNotifyPeer { peer: input }),
        NotifyScope::ForumTopic => tl::enums::InputNotifyPeer::InputNotifyForumTopic(
            tl::types::InputNotifyForumTopic {
                peer: input,
                top_msg_id: data
                    .top_msg_id
                    .ok_or("a forumTopic scope needs a topMsgId")?,
            },
        ),
        NotifyScope::Community => {
            let tl::enums::InputPeer::Channel(channel) = input else {
                return Err("a community scope needs a broadcast channel".to_owned());
            };
            tl::enums::InputNotifyPeer::InputNotifyCommunity(tl::types::InputNotifyCommunity {
                // The layer wants the bare channel here, not the peer form it resolved to.
                community: tl::enums::InputChannel::Channel(tl::types::InputChannel {
                    channel_id: channel.channel_id,
                    access_hash: channel.access_hash,
                }),
            })
        }
        // Guarded by `names_a_peer`.
        NotifyScope::Account | NotifyScope::Users | NotifyScope::Chats | NotifyScope::Broadcasts => {
            unreachable!("these scopes name no peer and are mapped above")
        }
    })
}

/// The layer's spelling of the account's own settings: the logged-in user.
fn notify_peer_self() -> tl::enums::InputNotifyPeer {
    tl::enums::InputNotifyPeer::Peer(tl::types::InputNotifyPeer {
        peer: tl::enums::InputPeer::PeerSelf,
    })
}

/// Maps the requested settings onto the layer's `inputPeerNotifySettings`.
fn notify_settings_input(
    spec: &NotifySettingsSpec,
) -> Result<tl::types::InputPeerNotifySettings, String> {
    Ok(tl::types::InputPeerNotifySettings {
        show_previews: spec.show_previews,
        silent: spec.silent,
        mute_until: spec.mute_until.map(seconds).transpose()?,
        sound: spec.sound.as_ref().map(notify_sound_input).transpose()?,
        stories_muted: spec.stories_muted,
        stories_hide_sender: spec.stories_hide_sender,
        stories_sound: spec.stories_sound.as_ref().map(notify_sound_input).transpose()?,
    })
}

/// Maps one requested sound onto the layer's `notificationSound`.
fn notify_sound_input(spec: &NotifySoundSpec) -> Result<tl::enums::NotificationSound, String> {
    Ok(match spec.kind.as_str() {
        "default" => tl::enums::NotificationSound::Default,
        "none" => tl::enums::NotificationSound::None,
        "local" => tl::enums::NotificationSound::Local(tl::types::NotificationSoundLocal {
            title: spec
                .title
                .clone()
                .ok_or("a local notification sound needs a title")?,
            data: spec
                .data
                .clone()
                .ok_or("a local notification sound needs its data")?,
        }),
        "ringtone" => tl::enums::NotificationSound::Ringtone(tl::types::NotificationSoundRingtone {
            id: spec
                .id
                .ok_or("a ringtone notification sound needs an id")?,
        }),
        other => {
            return Err(format!(
                "unsupported notification sound: {other}; expected default, none, local or ringtone"
            ))
        }
    })
}

/// The wire format is epoch milliseconds; the layer's `mute_until` is whole seconds.
fn seconds(millis: i64) -> Result<i32, String> {
    let seconds = millis / 1_000;
    i32::try_from(seconds).map_err(|_| {
        format!("muteUntil {millis} is out of the layer's date range")
    })
}

/// Maps a curated key name onto the layer's input key.
fn privacy_key_input(key: &str) -> Result<tl::enums::InputPrivacyKey, String> {
    if !PRIVACY_KEYS.contains(&key) {
        return Err(format!(
            "unsupported privacy key: {key}; expected one of {}",
            PRIVACY_KEYS.join(", ")
        ));
    }
    Ok(match key {
        "statusTimestamp" => tl::enums::InputPrivacyKey::StatusTimestamp,
        "chatInvite" => tl::enums::InputPrivacyKey::ChatInvite,
        "phoneNumber" => tl::enums::InputPrivacyKey::PhoneNumber,
        _ => unreachable!("every accepted key is mapped above"),
    })
}

/// Maps one requested rule onto the layer's input rule.
fn privacy_rule_input(spec: &PrivacyRuleSpec) -> Result<tl::enums::InputPrivacyRule, String> {
    Ok(match spec.kind.as_str() {
        "allowAll" => tl::enums::InputPrivacyRule::InputPrivacyValueAllowAll,
        "disallowAll" => tl::enums::InputPrivacyRule::InputPrivacyValueDisallowAll,
        "allowContacts" => tl::enums::InputPrivacyRule::InputPrivacyValueAllowContacts,
        "disallowContacts" => tl::enums::InputPrivacyRule::InputPrivacyValueDisallowContacts,
        "allowChatParticipants" => {
            tl::enums::InputPrivacyRule::InputPrivacyValueAllowChatParticipants(
                tl::types::InputPrivacyValueAllowChatParticipants {
                    chats: spec.chats.clone(),
                },
            )
        }
        "disallowChatParticipants" => {
            tl::enums::InputPrivacyRule::InputPrivacyValueDisallowChatParticipants(
                tl::types::InputPrivacyValueDisallowChatParticipants {
                    chats: spec.chats.clone(),
                },
            )
        }
        "allowCloseFriends" => tl::enums::InputPrivacyRule::InputPrivacyValueAllowCloseFriends,
        "allowPremium" => tl::enums::InputPrivacyRule::InputPrivacyValueAllowPremium,
        "allowBots" => tl::enums::InputPrivacyRule::InputPrivacyValueAllowBots,
        "disallowBots" => tl::enums::InputPrivacyRule::InputPrivacyValueDisallowBots,
        "allowUsers" | "disallowUsers" => {
            return Err(format!(
                "unsupported privacy rule: {}; the layer takes each user's access hash, \
                 which this operation does not carry — use invokeRaw",
                spec.kind
            ));
        }
        other => return Err(format!("unsupported privacy rule: {other}")),
    })
}

#[cfg(test)]
mod tests {
    //! Tests for the payloads and the two mappings, which are the only parts of the account
    //! operations that do not need a live session.

    use super::*;
    use std::ptr::fn_addr_eq;

    #[test]
    fn a_profile_payload_reads_camel_case_and_leaves_absent_fields_alone() {
        let full: UpdateProfilePayload =
            parse_payload(r#"{"firstName": "Some", "lastName": "One", "about": "hi"}"#)
                .expect("a profile payload");
        assert_eq!(full.first_name.as_deref(), Some("Some"));
        assert_eq!(full.last_name.as_deref(), Some("One"));
        assert_eq!(full.about.as_deref(), Some("hi"));

        let empty: UpdateProfilePayload = parse_payload("{}").expect("an empty profile payload");
        assert!(empty.first_name.is_none());
        assert!(empty.last_name.is_none());
        assert!(empty.about.is_none());
    }

    #[test]
    fn a_username_payload_reads_the_username() {
        let payload: UsernamePayload =
            parse_payload(r#"{"username": "someone"}"#).expect("a username payload");
        assert_eq!(payload.username, "someone");
    }

    #[test]
    fn an_update_status_payload_reads_the_offline_flag() {
        let payload: UpdateStatusPayload =
            parse_payload(r#"{"offline": true}"#).expect("a status payload");
        assert!(payload.offline);
    }

    #[test]
    fn a_reset_authorization_payload_reads_the_hash() {
        let payload: ResetAuthorizationPayload =
            parse_payload(r#"{"hash": 8867911212683051761}"#).expect("a reset payload");
        assert_eq!(payload.hash, 8_867_911_212_683_051_761);
    }

    #[test]
    fn a_privacy_payload_reads_the_key_and_the_rules() {
        let set: SetPrivacyPayload = parse_payload(
            r#"{"key": "statusTimestamp", "rules": [{"kind": "allowAll"},
                {"kind": "disallowChatParticipants", "chats": [42, 43]}]}"#,
        )
        .expect("a set privacy payload");
        assert_eq!(set.key, "statusTimestamp");
        assert_eq!(set.rules.len(), 2);
        assert_eq!(set.rules[1].chats, vec![42, 43]);

        let get: PrivacyKeyPayload =
            parse_payload(r#"{"key": "chatInvite"}"#).expect("a get privacy payload");
        assert_eq!(get.key, "chatInvite");
    }

    #[test]
    fn only_the_curated_privacy_keys_map_to_the_layer() {
        assert_eq!(
            privacy_key_input("statusTimestamp").expect("a key"),
            tl::enums::InputPrivacyKey::StatusTimestamp
        );
        assert_eq!(
            privacy_key_input("chatInvite").expect("a key"),
            tl::enums::InputPrivacyKey::ChatInvite
        );
        assert_eq!(
            privacy_key_input("phoneNumber").expect("a key"),
            tl::enums::InputPrivacyKey::PhoneNumber
        );
        let error = privacy_key_input("profilePhoto").expect_err("not curated");
        assert!(error.contains("unsupported privacy key: profilePhoto"));
    }

    #[test]
    fn every_valueless_privacy_rule_maps_to_its_layer_variant() {
        let cases = [
            (
                "allowAll",
                tl::enums::InputPrivacyRule::InputPrivacyValueAllowAll,
            ),
            (
                "disallowAll",
                tl::enums::InputPrivacyRule::InputPrivacyValueDisallowAll,
            ),
            (
                "allowContacts",
                tl::enums::InputPrivacyRule::InputPrivacyValueAllowContacts,
            ),
            (
                "disallowContacts",
                tl::enums::InputPrivacyRule::InputPrivacyValueDisallowContacts,
            ),
            (
                "allowCloseFriends",
                tl::enums::InputPrivacyRule::InputPrivacyValueAllowCloseFriends,
            ),
            (
                "allowPremium",
                tl::enums::InputPrivacyRule::InputPrivacyValueAllowPremium,
            ),
            (
                "allowBots",
                tl::enums::InputPrivacyRule::InputPrivacyValueAllowBots,
            ),
            (
                "disallowBots",
                tl::enums::InputPrivacyRule::InputPrivacyValueDisallowBots,
            ),
        ];
        for (kind, expected) in cases {
            assert_eq!(
                privacy_rule_input(&PrivacyRuleSpec {
                    kind: kind.to_owned(),
                    chats: vec![],
                })
                .expect("a rule"),
                expected,
                "unexpected mapping for {kind}"
            );
        }
    }

    #[test]
    fn a_chat_participant_rule_carries_its_chat_ids() {
        assert_eq!(
            privacy_rule_input(&PrivacyRuleSpec {
                kind: "allowChatParticipants".to_owned(),
                chats: vec![42],
            })
            .expect("a rule"),
            tl::enums::InputPrivacyRule::InputPrivacyValueAllowChatParticipants(
                tl::types::InputPrivacyValueAllowChatParticipants { chats: vec![42] }
            )
        );
    }

    #[test]
    fn a_user_rule_and_an_unknown_rule_are_refused() {
        let users = privacy_rule_input(&PrivacyRuleSpec {
            kind: "allowUsers".to_owned(),
            chats: vec![],
        })
        .expect_err("user rules need access hashes");
        assert!(users.contains("unsupported privacy rule: allowUsers"));

        let unknown = privacy_rule_input(&PrivacyRuleSpec {
            kind: "allowEveryone".to_owned(),
            chats: vec![],
        })
        .expect_err("no such rule");
        assert_eq!(unknown, "unsupported privacy rule: allowEveryone");
    }

    #[test]
    fn a_notify_payload_reads_its_scope_and_the_peer_it_names() {
        let account: GetNotifySettingsPayload =
            parse_payload(r#"{"scope": "account"}"#).expect("an account-wide payload");
        assert_eq!(account.scope, NotifyScope::Account);
        assert!(account.peer.peer_handle.is_none());
        assert!(account.peer.username.is_none());
        assert_eq!(account.top_msg_id, None);

        let peer: GetNotifySettingsPayload =
            parse_payload(r#"{"scope": "peer", "peerHandle": 42}"#).expect("a peer payload");
        assert_eq!(peer.scope, NotifyScope::Peer);
        assert_eq!(peer.peer.peer_handle, Some(42));
        assert!(peer.scope.names_a_peer());

        let topic: UpdateNotifySettingsPayload = parse_payload(
            r#"{"scope": "forumTopic", "username": "someForum", "topMsgId": 9,
                "settings": {"silent": true}}"#,
        )
        .expect("an update payload");
        assert_eq!(topic.target.scope, NotifyScope::ForumTopic);
        assert_eq!(topic.target.peer.username.as_deref(), Some("someForum"));
        assert_eq!(topic.target.top_msg_id, Some(9));
        assert_eq!(topic.settings.silent, Some(true));
        assert_eq!(topic.settings.mute_until, None);
    }

    #[test]
    fn a_notify_payload_without_a_scope_is_refused() {
        let error = match parse_payload::<GetNotifySettingsPayload>("{}") {
            Ok(_) => panic!("a payload without a scope names nothing to read"),
            Err(error) => error,
        };
        assert!(error.contains("scope"), "{error}");
    }

    #[test]
    fn every_scope_maps_to_its_wire_name() {
        let cases = [
            (NotifyScope::Account, "account"),
            (NotifyScope::Peer, "peer"),
            (NotifyScope::Users, "users"),
            (NotifyScope::Chats, "chats"),
            (NotifyScope::Broadcasts, "broadcasts"),
            (NotifyScope::ForumTopic, "forumTopic"),
            (NotifyScope::Community, "community"),
        ];
        for (scope, expected) in cases {
            assert_eq!(scope.name(), expected);
            let payload: GetNotifySettingsPayload = parse_payload(&format!(
                r#"{{"scope": "{expected}"}}"#
            ))
            .expect("a named scope");
            assert_eq!(payload.scope, scope);
        }
    }

    #[test]
    fn the_account_scope_is_the_logged_in_user_and_the_others_name_no_peer() {
        assert_eq!(
            notify_peer_self(),
            tl::enums::InputNotifyPeer::Peer(tl::types::InputNotifyPeer {
                peer: tl::enums::InputPeer::PeerSelf,
            })
        );
        assert!(!NotifyScope::Account.names_a_peer());
        assert!(!NotifyScope::Users.names_a_peer());
        assert!(!NotifyScope::Chats.names_a_peer());
        assert!(!NotifyScope::Broadcasts.names_a_peer());
    }

    #[test]
    fn the_settings_map_onto_the_layer_flags_and_convert_the_mute_date() {
        let spec: NotifySettingsSpec = parse_payload(
            r#"{"showPreviews": false, "silent": true, "muteUntil": 1700000000000,
                "storiesMuted": true, "storiesHideSender": false}"#,
        )
        .expect("a settings spec");
        let settings = notify_settings_input(&spec).expect("the settings map");
        assert_eq!(settings.show_previews, Some(false));
        assert_eq!(settings.silent, Some(true));
        assert_eq!(settings.mute_until, Some(1_700_000_000));
        assert_eq!(settings.stories_muted, Some(true));
        assert_eq!(settings.stories_hide_sender, Some(false));
        assert_eq!(settings.sound, None);
        assert_eq!(settings.stories_sound, None);

        // An empty spec changes nothing at all, which is how one setting is written without
        // resetting the rest.
        let settings = notify_settings_input(&NotifySettingsSpec {
            show_previews: None,
            silent: None,
            mute_until: None,
            sound: None,
            stories_muted: None,
            stories_hide_sender: None,
            stories_sound: None,
        })
        .expect("an empty spec");
        assert_eq!(settings.show_previews, None);
        assert_eq!(settings.silent, None);
        assert_eq!(settings.mute_until, None);
    }

    #[test]
    fn a_mute_date_outside_the_layer_range_is_refused() {
        // A whole second past the last one the layer's 32-bit date can hold.
        let error = seconds(i64::from(i32::MAX) * 1_000 + 1_000).expect_err("out of range");
        assert!(error.contains("out of the layer's date range"), "{error}");
        // The last second it can hold is still fine.
        assert_eq!(seconds(i64::from(i32::MAX) * 1_000), Ok(i32::MAX));
        assert_eq!(seconds(1_700_000_000_000), Ok(1_700_000_000));
        assert_eq!(seconds(0), Ok(0));
        assert_eq!(seconds(1_500), Ok(1));
    }

    #[test]
    fn every_sound_kind_maps_to_its_layer_constructor() {
        let sound = |kind: &str| NotifySoundSpec {
            kind: kind.to_owned(),
            id: Some(5150),
            title: Some("Ping".to_owned()),
            data: Some("blob".to_owned()),
        };
        assert_eq!(
            notify_sound_input(&sound("default")).expect("default"),
            tl::enums::NotificationSound::Default
        );
        assert_eq!(
            notify_sound_input(&sound("none")).expect("none"),
            tl::enums::NotificationSound::None
        );
        assert_eq!(
            notify_sound_input(&sound("local")).expect("local"),
            tl::enums::NotificationSound::Local(tl::types::NotificationSoundLocal {
                title: "Ping".to_owned(),
                data: "blob".to_owned(),
            })
        );
        assert_eq!(
            notify_sound_input(&sound("ringtone")).expect("ringtone"),
            tl::enums::NotificationSound::Ringtone(tl::types::NotificationSoundRingtone { id: 5150 })
        );
    }

    #[test]
    fn a_sound_missing_its_field_and_an_unknown_kind_are_refused() {
        let local = NotifySoundSpec {
            kind: "local".to_owned(),
            id: None,
            title: None,
            data: None,
        };
        let error = notify_sound_input(&local).expect_err("a local sound needs a title");
        assert!(error.contains("a local notification sound needs a title"), "{error}");

        let ringtone = NotifySoundSpec {
            kind: "ringtone".to_owned(),
            id: None,
            title: None,
            data: None,
        };
        let error = notify_sound_input(&ringtone).expect_err("a ringtone needs an id");
        assert!(error.contains("a ringtone notification sound needs an id"), "{error}");

        let unknown = NotifySoundSpec {
            kind: "bird".to_owned(),
            id: None,
            title: None,
            data: None,
        };
        assert_eq!(
            notify_sound_input(&unknown).expect_err("no such sound"),
            "unsupported notification sound: bird; expected default, none, local or ringtone"
        );
    }

    #[test]
    fn the_account_operations_route_to_their_own_handler() {
        assert!(fn_addr_eq(
            route("accountUpdateProfile").expect("routed"),
            update_profile as Handler,
        ));
        assert!(fn_addr_eq(
            route("accountGetPassword").expect("routed"),
            get_password as Handler,
        ));
        assert!(fn_addr_eq(
            route("accountSetPrivacy").expect("routed"),
            set_privacy as Handler,
        ));
        assert!(fn_addr_eq(
            route("accountGetNotifySettings").expect("routed"),
            get_notify_settings as Handler,
        ));
        assert!(fn_addr_eq(
            route("accountUpdateNotifySettings").expect("routed"),
            update_notify_settings as Handler,
        ));
    }

    #[test]
    fn a_foreign_operation_is_not_routed_by_this_module() {
        assert!(route("getMe").is_none());
        assert!(route("accountDeleteAccount").is_none());
    }
}
