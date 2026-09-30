//! User projection.

use grammers_client::peer::{Platform, User as ClientUser};
use grammers_client::tl;
use serde::Serialize;

/// A Telegram account, mirroring the accessors grammers exposes on [`ClientUser`].
///
/// grammers has no accessor for the layer's `premium`, `fake`, `bot_info_version` or
/// `bot_description` flags, so those are absent rather than read out of the raw user.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UserDto {
    pub(crate) id: i64,
    pub(crate) username: Option<String>,
    pub(crate) first_name: Option<String>,
    pub(crate) last_name: Option<String>,
    /// First and last name joined, or just the first name when there is no last name.
    pub(crate) full_name: String,
    /// The collectible usernames, which grammers reports separately from [Self::username].
    pub(crate) usernames: Vec<String>,
    pub(crate) phone: Option<String>,
    /// The identifier of the profile photo, for chats and profile-photo downloads.
    pub(crate) photo_id: Option<i64>,
    /// The grammers presence status, lowerCamelCased; `unknown` for `userStatusEmpty`.
    pub(crate) status: &'static str,
    /// Epoch milliseconds at which an `online` status lapses into `offline`.
    pub(crate) status_expires: Option<i64>,
    /// Epoch milliseconds of the last time an `offline` user was seen.
    pub(crate) last_seen: Option<i64>,
    /// True when a coarse status (`recently`, `lastWeek`, `lastMonth`) is relative to the viewer.
    pub(crate) status_by_me: bool,
    pub(crate) lang_code: Option<String>,
    pub(crate) is_self: bool,
    pub(crate) contact: bool,
    pub(crate) mutual_contact: bool,
    pub(crate) deleted: bool,
    pub(crate) is_bot: bool,
    pub(crate) bot_privacy: bool,
    pub(crate) bot_supports_chats: bool,
    pub(crate) bot_inline_geo: bool,
    pub(crate) bot_inline_placeholder: Option<String>,
    pub(crate) verified: bool,
    pub(crate) restricted: bool,
    pub(crate) support: bool,
    pub(crate) scam: bool,
    pub(crate) restriction_reasons: Vec<RestrictionReasonDto>,
}

/// One reason a user is restricted, mirroring grammers' `RestrictionReason`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RestrictionReasonDto {
    /// `all`, `android`, `ios`, `windowsPhone`, or the platform name Telegram reported.
    pub(crate) platforms: Vec<String>,
    pub(crate) reason: String,
    pub(crate) text: String,
}

/// Projects a grammers user. Used by the auth results and by chat participants.
pub(crate) fn user_dto(user: &ClientUser) -> UserDto {
    let (status, status_expires, last_seen, status_by_me) = status_dto(user.status());
    UserDto {
        id: user.id().bare_id().unwrap_or(0),
        username: user.username().map(ToOwned::to_owned),
        first_name: user.first_name().map(ToOwned::to_owned),
        last_name: user.last_name().map(ToOwned::to_owned),
        full_name: user.full_name(),
        usernames: strings(user.usernames()),
        phone: user.phone().map(ToOwned::to_owned),
        photo_id: user.photo().map(|photo| photo.photo_id),
        status,
        status_expires,
        last_seen,
        status_by_me,
        lang_code: user.lang_code().map(ToOwned::to_owned),
        is_self: user.is_self(),
        contact: user.contact(),
        mutual_contact: user.mutual_contact(),
        deleted: user.deleted(),
        is_bot: user.is_bot(),
        bot_privacy: user.bot_privacy(),
        // grammers' `bot_supports_chats` takes the user by value; every other accessor borrows.
        bot_supports_chats: user.clone().bot_supports_chats(),
        bot_inline_geo: user.bot_inline_geo(),
        bot_inline_placeholder: user.bot_inline_placeholder().map(ToOwned::to_owned),
        verified: user.verified(),
        restricted: user.restricted(),
        support: user.support(),
        scam: user.scam(),
        restriction_reasons: user
            .restriction_reason()
            .iter()
            .map(|reason| RestrictionReasonDto {
                platforms: reason.platforms.iter().map(platform_name).collect(),
                reason: reason.reason.clone(),
                text: reason.text.clone(),
            })
            .collect(),
    }
}

/// Names a grammers presence status and pulls out the one date each kind carries.
fn status_dto(status: &tl::enums::UserStatus) -> (&'static str, Option<i64>, Option<i64>, bool) {
    match status {
        tl::enums::UserStatus::Empty => ("unknown", None, None, false),
        tl::enums::UserStatus::Online(status) => {
            ("online", Some(millis(status.expires)), None, false)
        }
        tl::enums::UserStatus::Offline(status) => {
            ("offline", None, Some(millis(status.was_online)), false)
        }
        tl::enums::UserStatus::Recently(status) => ("recently", None, None, status.by_me),
        tl::enums::UserStatus::LastWeek(status) => ("lastWeek", None, None, status.by_me),
        tl::enums::UserStatus::LastMonth(status) => ("lastMonth", None, None, status.by_me),
    }
}

/// Names a platform a restriction applies to. `Platform` is `#[non_exhaustive]`, so a platform
/// added after this bridge was written is reported under its grammers variant.
fn platform_name(platform: &Platform) -> String {
    match platform {
        Platform::All => "all".to_owned(),
        Platform::Android => "android".to_owned(),
        Platform::Ios => "ios".to_owned(),
        Platform::WindowsPhone => "windowsPhone".to_owned(),
        Platform::Other(name) => name.clone(),
        _ => "unknown".to_owned(),
    }
}

/// The wire format for dates is epoch milliseconds, while the layer reports whole seconds.
fn millis(seconds: i32) -> i64 {
    i64::from(seconds) * 1_000
}

fn strings(values: Vec<&str>) -> Vec<String> {
    values.into_iter().map(ToOwned::to_owned).collect()
}
