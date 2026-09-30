//! Account authorization, password and privacy projections.
//!
//! grammers has no typed account-management surface, so [`super::super::ops::account`] drives the
//! layer's `account.*` functions directly and the results are projected here.
//!
//! The layer reports the two dates an authorization carries in whole seconds; like the user
//! projection, they travel as epoch milliseconds. The password projection mirrors the fields of
//! `account.password` that describe *whether* a password is set rather than the key material
//! needed to change it: `currentAlgo`, `srpB`, `srpId`, `newAlgo`, `newSecureAlgo` and
//! `secureRandom` are deliberately absent, because a caller that can read them can compute the
//! password hash, and this bridge exposes no operation that would take one.

use grammers_client::tl;
use serde::Serialize;

/// One active session on the account, mirroring the layer's `authorization` constructor.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AuthorizationDto {
    /// True for the session this call was made from.
    pub(crate) current: bool,
    pub(crate) official_app: bool,
    /// True when the session is waiting for a two-factor password to be confirmed.
    pub(crate) password_pending: bool,
    pub(crate) encrypted_requests_disabled: bool,
    pub(crate) call_requests_disabled: bool,
    /// True when the session has not confirmed its login yet.
    pub(crate) unconfirmed: bool,
    /// The identifier `account.resetAuthorization` takes.
    pub(crate) hash: i64,
    pub(crate) device_model: String,
    pub(crate) platform: String,
    pub(crate) system_version: String,
    pub(crate) api_id: i32,
    pub(crate) app_name: String,
    pub(crate) app_version: String,
    /// Epoch milliseconds at which the session was created.
    pub(crate) date_created: i64,
    /// Epoch milliseconds at which the session was last active.
    pub(crate) date_active: i64,
    pub(crate) ip: String,
    pub(crate) country: String,
    pub(crate) region: String,
}

/// The account's active sessions together with the day window Telegram reports for them.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AuthorizationsDto {
    /// How many days a session may stay inactive before Telegram drops it; `0` is no limit.
    pub(crate) authorization_ttl_days: i32,
    pub(crate) authorizations: Vec<AuthorizationDto>,
}

/// Whether the account has a two-factor password, and what Telegram reports about it.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PasswordSettingsDto {
    pub(crate) has_password: bool,
    pub(crate) has_recovery: bool,
    pub(crate) has_secure_values: bool,
    /// The hint Telegram shows when the password is requested.
    pub(crate) hint: Option<String>,
    /// The email pattern Telegram would send a recovery code to, with the address masked.
    pub(crate) email_unconfirmed_pattern: Option<String>,
    /// The login-email pattern, which is a newer alternative to the recovery email.
    pub(crate) login_email_pattern: Option<String>,
    /// Epoch milliseconds at which a pending password reset becomes effective.
    pub(crate) pending_reset_date: Option<i64>,
}

/// One rule of a privacy setting, mirroring one `privacyRule` constructor.
///
/// [kind] names the layer constructor lowerCamelCased, less its `privacyValue` prefix. The id
/// vectors are populated only for the `*Users` and `*ChatParticipants` kinds.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PrivacyRuleDto {
    pub(crate) kind: &'static str,
    /// The bare user identifiers an `*Users` rule carries.
    pub(crate) users: Vec<i64>,
    /// The bare chat identifiers a `*ChatParticipants` rule carries.
    pub(crate) chats: Vec<i64>,
}

/// The rules a privacy setting currently holds, together with the entities they reference.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PrivacyRulesDto {
    /// The curated key name that was asked for, as [`super::super::ops::account`] names it.
    pub(crate) key: String,
    pub(crate) rules: Vec<PrivacyRuleDto>,
    /// The bare chat identifiers of the chats the rules reference.
    pub(crate) chats: Vec<i64>,
    /// The bare user identifiers of the users the rules reference.
    pub(crate) users: Vec<i64>,
}

/// Projects the account's active sessions.
pub(crate) fn authorizations_dto(
    authorizations: &tl::types::account::Authorizations,
) -> AuthorizationsDto {
    AuthorizationsDto {
        authorization_ttl_days: authorizations.authorization_ttl_days,
        authorizations: authorizations
            .authorizations
            .iter()
            .map(authorization_dto)
            .collect(),
    }
}

/// Projects one active session.
pub(crate) fn authorization_dto(authorization: &tl::enums::Authorization) -> AuthorizationDto {
    let tl::enums::Authorization::Authorization(authorization) = authorization;
    AuthorizationDto {
        current: authorization.current,
        official_app: authorization.official_app,
        password_pending: authorization.password_pending,
        encrypted_requests_disabled: authorization.encrypted_requests_disabled,
        call_requests_disabled: authorization.call_requests_disabled,
        unconfirmed: authorization.unconfirmed,
        hash: authorization.hash,
        device_model: authorization.device_model.clone(),
        platform: authorization.platform.clone(),
        system_version: authorization.system_version.clone(),
        api_id: authorization.api_id,
        app_name: authorization.app_name.clone(),
        app_version: authorization.app_version.clone(),
        date_created: millis(authorization.date_created),
        date_active: millis(authorization.date_active),
        ip: authorization.ip.clone(),
        country: authorization.country.clone(),
        region: authorization.region.clone(),
    }
}

/// Projects the password settings of the account.
pub(crate) fn password_settings_dto(
    password: &tl::types::account::Password,
) -> PasswordSettingsDto {
    PasswordSettingsDto {
        has_password: password.has_password,
        has_recovery: password.has_recovery,
        has_secure_values: password.has_secure_values,
        hint: password.hint.clone(),
        email_unconfirmed_pattern: password.email_unconfirmed_pattern.clone(),
        login_email_pattern: password.login_email_pattern.clone(),
        pending_reset_date: password.pending_reset_date.map(millis),
    }
}

/// Projects the rules of a privacy setting under the curated [key] name that was asked for.
pub(crate) fn privacy_rules_dto(
    key: &str,
    rules: &tl::types::account::PrivacyRules,
) -> PrivacyRulesDto {
    PrivacyRulesDto {
        key: key.to_owned(),
        rules: rules.rules.iter().map(privacy_rule_dto).collect(),
        chats: rules.chats.iter().map(|chat| chat.id()).collect(),
        users: rules.users.iter().map(|user| user.id()).collect(),
    }
}

/// Projects one privacy rule, naming the layer constructor it came from.
pub(crate) fn privacy_rule_dto(rule: &tl::enums::PrivacyRule) -> PrivacyRuleDto {
    let (kind, users, chats) = match rule {
        tl::enums::PrivacyRule::PrivacyValueAllowContacts => ("allowContacts", vec![], vec![]),
        tl::enums::PrivacyRule::PrivacyValueAllowAll => ("allowAll", vec![], vec![]),
        tl::enums::PrivacyRule::PrivacyValueAllowUsers(users) => {
            ("allowUsers", users.users.clone(), vec![])
        }
        tl::enums::PrivacyRule::PrivacyValueDisallowContacts => {
            ("disallowContacts", vec![], vec![])
        }
        tl::enums::PrivacyRule::PrivacyValueDisallowAll => ("disallowAll", vec![], vec![]),
        tl::enums::PrivacyRule::PrivacyValueDisallowUsers(users) => {
            ("disallowUsers", users.users.clone(), vec![])
        }
        tl::enums::PrivacyRule::PrivacyValueAllowChatParticipants(chats) => {
            ("allowChatParticipants", vec![], chats.chats.clone())
        }
        tl::enums::PrivacyRule::PrivacyValueDisallowChatParticipants(chats) => {
            ("disallowChatParticipants", vec![], chats.chats.clone())
        }
        tl::enums::PrivacyRule::PrivacyValueAllowCloseFriends => {
            ("allowCloseFriends", vec![], vec![])
        }
        tl::enums::PrivacyRule::PrivacyValueAllowPremium => ("allowPremium", vec![], vec![]),
        tl::enums::PrivacyRule::PrivacyValueAllowBots => ("allowBots", vec![], vec![]),
        tl::enums::PrivacyRule::PrivacyValueDisallowBots => ("disallowBots", vec![], vec![]),
    };
    PrivacyRuleDto { kind, users, chats }
}

/// The layer reports dates in whole seconds; the wire format is epoch milliseconds.
fn millis(seconds: i32) -> i64 {
    i64::from(seconds) * 1_000
}

#[cfg(test)]
mod tests {
    //! Projection tests for the account DTOs, built from layer values by hand so every field is
    //! pinned without a live session.

    use super::*;
    use serde::Serialize;
    use serde_json::json;

    /// Asserts that [dto] encodes to exactly [expected].
    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    /// An `authorization` with every field populated.
    fn authorization() -> tl::enums::Authorization {
        tl::enums::Authorization::Authorization(tl::types::Authorization {
            current: true,
            official_app: true,
            password_pending: false,
            encrypted_requests_disabled: true,
            call_requests_disabled: false,
            unconfirmed: false,
            hash: 8_867_911_212_683_051_761,
            device_model: "Pixel 9".to_owned(),
            platform: "Android".to_owned(),
            system_version: "15".to_owned(),
            api_id: 2040,
            app_name: "Kotlogram".to_owned(),
            app_version: "1.2.3".to_owned(),
            date_created: 1_700_000_000,
            date_active: 1_700_000_100,
            ip: "203.0.113.7".to_owned(),
            country: "Ireland".to_owned(),
            region: "Leinster".to_owned(),
        })
    }

    #[test]
    fn an_authorization_encodes_every_session_field() {
        assert_json(
            &authorization_dto(&authorization()),
            json!({
                "current": true,
                "officialApp": true,
                "passwordPending": false,
                "encryptedRequestsDisabled": true,
                "callRequestsDisabled": false,
                "unconfirmed": false,
                "hash": 8_867_911_212_683_051_761i64,
                "deviceModel": "Pixel 9",
                "platform": "Android",
                "systemVersion": "15",
                "apiId": 2040,
                "appName": "Kotlogram",
                "appVersion": "1.2.3",
                "dateCreated": 1_700_000_000_000i64,
                "dateActive": 1_700_000_100_000i64,
                "ip": "203.0.113.7",
                "country": "Ireland",
                "region": "Leinster",
            }),
        );
    }

    #[test]
    fn an_authorizations_document_carries_the_window_and_the_list() {
        let dto = authorizations_dto(&tl::types::account::Authorizations {
            authorization_ttl_days: 180,
            authorizations: vec![authorization()],
        });
        assert_eq!(dto.authorization_ttl_days, 180);
        assert_eq!(dto.authorizations.len(), 1);
        assert!(dto.authorizations[0].current);
    }

    #[test]
    fn empty_authorizations_still_carry_their_window() {
        assert_json(
            &authorizations_dto(&tl::types::account::Authorizations {
                authorization_ttl_days: 0,
                authorizations: vec![],
            }),
            json!({ "authorizationTtlDays": 0, "authorizations": [] }),
        );
    }

    #[test]
    fn password_settings_encodes_every_descriptive_field() {
        let password = tl::types::account::Password {
            has_recovery: true,
            has_secure_values: true,
            has_password: true,
            current_algo: None,
            srp_b: None,
            srp_id: None,
            hint: Some("mother".to_owned()),
            email_unconfirmed_pattern: Some("a***@example.com".to_owned()),
            new_algo: tl::enums::PasswordKdfAlgo::Unknown,
            new_secure_algo: tl::enums::SecurePasswordKdfAlgo::Unknown,
            secure_random: vec![],
            pending_reset_date: Some(1_700_000_000),
            login_email_pattern: Some("l***@example.com".to_owned()),
        };
        assert_json(
            &password_settings_dto(&password),
            json!({
                "hasPassword": true,
                "hasRecovery": true,
                "hasSecureValues": true,
                "hint": "mother",
                "emailUnconfirmedPattern": "a***@example.com",
                "loginEmailPattern": "l***@example.com",
                "pendingResetDate": 1_700_000_000_000i64,
            }),
        );
    }

    #[test]
    fn password_settings_leave_absent_fields_null() {
        let password = tl::types::account::Password {
            has_recovery: false,
            has_secure_values: false,
            has_password: false,
            current_algo: None,
            srp_b: None,
            srp_id: None,
            hint: None,
            email_unconfirmed_pattern: None,
            new_algo: tl::enums::PasswordKdfAlgo::Unknown,
            new_secure_algo: tl::enums::SecurePasswordKdfAlgo::Unknown,
            secure_random: vec![],
            pending_reset_date: None,
            login_email_pattern: None,
        };
        assert_json(
            &password_settings_dto(&password),
            json!({
                "hasPassword": false,
                "hasRecovery": false,
                "hasSecureValues": false,
                "hint": null,
                "emailUnconfirmedPattern": null,
                "loginEmailPattern": null,
                "pendingResetDate": null,
            }),
        );
    }

    #[test]
    fn a_privacy_rule_names_its_constructor_and_its_ids() {
        assert_eq!(
            privacy_rule_dto(&tl::enums::PrivacyRule::PrivacyValueAllowAll).kind,
            "allowAll"
        );
        assert_eq!(
            privacy_rule_dto(&tl::enums::PrivacyRule::PrivacyValueAllowCloseFriends).kind,
            "allowCloseFriends"
        );

        let users = privacy_rule_dto(&tl::enums::PrivacyRule::PrivacyValueAllowUsers(
            tl::types::PrivacyValueAllowUsers { users: vec![7, 8] },
        ));
        assert_eq!(users.kind, "allowUsers");
        assert_eq!(users.users, vec![7, 8]);
        assert!(users.chats.is_empty());

        let chats = privacy_rule_dto(
            &tl::enums::PrivacyRule::PrivacyValueDisallowChatParticipants(
                tl::types::PrivacyValueDisallowChatParticipants { chats: vec![42] },
            ),
        );
        assert_eq!(chats.kind, "disallowChatParticipants");
        assert_eq!(chats.chats, vec![42]);
        assert!(chats.users.is_empty());
    }

    #[test]
    fn a_privacy_rules_document_carries_the_key_and_the_entities() {
        let dto = privacy_rules_dto(
            "statusTimestamp",
            &tl::types::account::PrivacyRules {
                rules: vec![
                    tl::enums::PrivacyRule::PrivacyValueAllowContacts,
                    tl::enums::PrivacyRule::PrivacyValueDisallowUsers(
                        tl::types::PrivacyValueDisallowUsers { users: vec![7] },
                    ),
                ],
                chats: vec![],
                users: vec![tl::enums::User::Empty(tl::types::UserEmpty { id: 7 })],
            },
        );
        assert_eq!(dto.key, "statusTimestamp");
        assert_eq!(dto.rules.len(), 2);
        assert_eq!(dto.rules[0].kind, "allowContacts");
        assert_eq!(dto.users, vec![7]);
        assert!(dto.chats.is_empty());
    }
}
