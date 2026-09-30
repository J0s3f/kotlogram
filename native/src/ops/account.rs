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

use grammers_client::peer::User as ClientUser;
use grammers_client::tl;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::NativeClient;
use crate::dto::account::{authorizations_dto, password_settings_dto, privacy_rules_dto};
use crate::dto::user_dto;
use crate::error::{invocation_error, json_string, parse_payload};

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

/// Projects a layer user an account update answered with.
fn project_user(native: &NativeClient, user: tl::enums::User) -> Result<String, String> {
    let user = ClientUser::from_raw(&native.client, user);
    json_string(user_dto(&user))
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
    fn the_account_operations_route_to_their_own_handler() {
        assert_eq!(
            route("accountUpdateProfile"),
            Some(update_profile as Handler)
        );
        assert_eq!(route("accountGetPassword"), Some(get_password as Handler));
        assert_eq!(route("accountSetPrivacy"), Some(set_privacy as Handler));
    }

    #[test]
    fn a_foreign_operation_is_not_routed_by_this_module() {
        assert!(route("getMe").is_none());
        assert!(route("accountDeleteAccount").is_none());
    }
}
