//! Account-identity projection.
//!
//! grammers exposes two things about the signed-in account: the account itself, through
//! `Client::get_me`, and the data centre its session is homed on, through the session's
//! `home_dc_id`. Neither is a chat or a message, so both are projected here.
//!
//! The data-centre identifier is the number grammers executes the session's main queries against.
//! It is not a chat id and has no access hash, so it travels as a plain integer.

use grammers_client::types::User as ClientUser;
use serde::Serialize;

use crate::dto::user::{user_dto, UserDto};

/// The signed-in account together with the data centre its session is homed on.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct MeDto {
    /// The full account projection, as [`ClientUser`] describes it.
    pub(crate) user: UserDto,
    /// The home data centre of the session, which is what a later raw call defaults to.
    pub(crate) data_centre_id: i32,
}

/// The home data centre of the session on its own.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct DataCentreDto {
    pub(crate) data_centre_id: i32,
}

/// Projects the signed-in account together with the data centre it lives on.
pub(crate) fn me_dto(user: &ClientUser, data_centre_id: i32) -> MeDto {
    MeDto {
        user: user_dto(user),
        data_centre_id,
    }
}

/// Projects the data-centre identifier a session reports.
pub(crate) fn data_centre_dto(data_centre_id: i32) -> DataCentreDto {
    DataCentreDto { data_centre_id }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dto::user::RestrictionReasonDto;
    use serde_json::json;

    /// Asserts that [dto] encodes to exactly [expected].
    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    /// A user with every projected field populated, so no field can change silently.
    fn full_user() -> UserDto {
        UserDto {
            id: 7,
            username: Some("someone".to_owned()),
            first_name: Some("Some".to_owned()),
            last_name: Some("One".to_owned()),
            full_name: "Some One".to_owned(),
            usernames: vec!["someone_alt".to_owned()],
            phone: Some("+10000000000".to_owned()),
            photo_id: Some(4242),
            status: "offline",
            status_expires: None,
            last_seen: Some(1_700_000_000_000),
            status_by_me: false,
            lang_code: Some("en".to_owned()),
            is_self: true,
            contact: true,
            mutual_contact: false,
            deleted: false,
            is_bot: false,
            bot_privacy: false,
            bot_supports_chats: false,
            bot_inline_geo: false,
            bot_inline_placeholder: None,
            verified: false,
            restricted: false,
            support: false,
            scam: false,
            restriction_reasons: vec![],
        }
    }

    #[test]
    fn me_encodes_the_account_and_its_data_centre() {
        let mut expected = json!({ "dataCentreId": 2 });
        expected.as_object_mut().expect("an object").insert(
            "user".to_owned(),
            serde_json::to_value(full_user()).expect("a user encodes"),
        );
        assert_json(&me_dto_from(full_user(), 2), expected);
    }

    #[test]
    fn me_encodes_the_full_user_not_just_its_identifier() {
        let dto = me_dto_from(full_user(), 2);
        assert_json(
            &dto.user,
            json!({
                "id": 7,
                "username": "someone",
                "firstName": "Some",
                "lastName": "One",
                "fullName": "Some One",
                "usernames": ["someone_alt"],
                "phone": "+10000000000",
                "photoId": 4242,
                "status": "offline",
                "statusExpires": null,
                "lastSeen": 1_700_000_000_000i64,
                "statusByMe": false,
                "langCode": "en",
                "isSelf": true,
                "contact": true,
                "mutualContact": false,
                "deleted": false,
                "isBot": false,
                "botPrivacy": false,
                "botSupportsChats": false,
                "botInlineGeo": false,
                "botInlinePlaceholder": null,
                "verified": false,
                "restricted": false,
                "support": false,
                "scam": false,
                "restrictionReasons": [],
            }),
        );
    }

    #[test]
    fn a_data_centre_encodes_just_its_identifier() {
        assert_json(&data_centre_dto(4), json!({ "dataCentreId": 4 }));
    }

    /// Builds a [MeDto] from a user value by hand, since [`me_dto`] needs a live grammers user.
    fn me_dto_from(user: UserDto, data_centre_id: i32) -> MeDto {
        MeDto {
            user,
            data_centre_id,
        }
    }

    #[test]
    fn a_restriction_reason_still_projects_through_the_account() {
        let user = UserDto {
            restriction_reasons: vec![RestrictionReasonDto {
                platforms: vec!["all".to_owned()],
                reason: "spam".to_owned(),
                text: "Reported as spam".to_owned(),
            }],
            ..full_user()
        };
        let encoded = serde_json::to_value(me_dto_from(user, 2)).expect("a projection encodes");
        assert_eq!(
            encoded["user"]["restrictionReasons"],
            json!([{ "platforms": ["all"], "reason": "spam", "text": "Reported as spam" }]),
        );
    }
}
