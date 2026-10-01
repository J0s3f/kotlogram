//! Notification-setting projections.
//!
//! grammers types no notification settings of its own, so [`super::super::ops::account`] drives the
//! layer's `account.getNotifySettings`/`account.updateNotifySettings` directly and the answers are
//! projected here.
//!
//! The layer reports `mute_until` in whole seconds; like every other date on this wire it travels as
//! epoch milliseconds. The per-platform sounds are kept as the four constructors the layer has —
//! `default`, `none`, a local file and a system ringtone — rather than flattened into one string,
//! because a caller choosing a sound needs its title and data back.
//!
//! [`super::dialog_meta`] projects the same layer type for the dialog listing, but only the three
//! settings a client reads while walking a dialog. This projection is the complete one, because it
//! answers a question about one scope rather than annotating a listing.

use grammers_client::tl;
use serde::Serialize;

/// One notification sound, mirroring one `notificationSound` constructor.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct NotificationSoundDto {
    /// The layer constructor lowerCamelCased: `default`, `none`, `local` or `ringtone`.
    pub(crate) kind: &'static str,
    /// A ringtone's identifier; set only for a `ringtone` sound.
    pub(crate) id: Option<i64>,
    /// A local sound's shown title; set only for a `local` sound.
    pub(crate) title: Option<String>,
    /// A local sound's data blob, which the layer hands out as an opaque string; only for `local`.
    pub(crate) data: Option<String>,
}

/// One scope's notification settings, mirroring the layer's `peerNotifySettings`.
///
/// Every field is optional because the layer's settings are flags: an absent one is not "false" but
/// "Telegram did not say", which a caller cannot tell apart from an explicit false on this layer.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PeerNotifySettingsDto {
    pub(crate) show_previews: Option<bool>,
    pub(crate) silent: Option<bool>,
    /// Epoch milliseconds, converted from the layer's whole-second `mute_until`.
    pub(crate) mute_until: Option<i64>,
    pub(crate) ios_sound: Option<NotificationSoundDto>,
    pub(crate) android_sound: Option<NotificationSoundDto>,
    pub(crate) other_sound: Option<NotificationSoundDto>,
    /// Whether stories from this scope are muted.
    pub(crate) stories_muted: Option<bool>,
    /// Whether the story's author is hidden.
    pub(crate) stories_hide_sender: Option<bool>,
    pub(crate) stories_ios_sound: Option<NotificationSoundDto>,
    pub(crate) stories_android_sound: Option<NotificationSoundDto>,
    pub(crate) stories_other_sound: Option<NotificationSoundDto>,
}

/// The `accountGetNotifySettings` document: the settings in place for one notify scope.
///
/// The layer answers the bare `peerNotifySettings` with no room for which scope was asked for, so
/// [scope] is the bridge's own echo of the name the request used. Without it an answer about the
/// whole account and one about a single chat would be indistinguishable.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct NotifySettingsDto {
    /// The scope that was asked for, as [`super::super::ops::account`] names it.
    pub(crate) scope: &'static str,
    /// The settings the layer reported for that scope.
    pub(crate) settings: PeerNotifySettingsDto,
}

/// Projects one `notificationSound`.
pub(crate) fn notification_sound_dto(sound: &tl::enums::NotificationSound) -> NotificationSoundDto {
    match sound {
        tl::enums::NotificationSound::Default => NotificationSoundDto {
            kind: "default",
            id: None,
            title: None,
            data: None,
        },
        tl::enums::NotificationSound::None => NotificationSoundDto {
            kind: "none",
            id: None,
            title: None,
            data: None,
        },
        tl::enums::NotificationSound::Local(local) => NotificationSoundDto {
            kind: "local",
            id: None,
            title: Some(local.title.clone()),
            data: Some(local.data.clone()),
        },
        tl::enums::NotificationSound::Ringtone(ringtone) => NotificationSoundDto {
            kind: "ringtone",
            id: Some(ringtone.id),
            title: None,
            data: None,
        },
    }
}

/// Projects the layer's notification settings.
pub(crate) fn peer_notify_settings_dto(
    settings: &tl::enums::PeerNotifySettings,
) -> PeerNotifySettingsDto {
    let tl::enums::PeerNotifySettings::Settings(settings) = settings;
    PeerNotifySettingsDto {
        show_previews: settings.show_previews,
        silent: settings.silent,
        mute_until: settings.mute_until.map(millis),
        ios_sound: settings.ios_sound.as_ref().map(notification_sound_dto),
        android_sound: settings.android_sound.as_ref().map(notification_sound_dto),
        other_sound: settings.other_sound.as_ref().map(notification_sound_dto),
        stories_muted: settings.stories_muted,
        stories_hide_sender: settings.stories_hide_sender,
        stories_ios_sound: settings.stories_ios_sound.as_ref().map(notification_sound_dto),
        stories_android_sound: settings
            .stories_android_sound
            .as_ref()
            .map(notification_sound_dto),
        stories_other_sound: settings.stories_other_sound.as_ref().map(notification_sound_dto),
    }
}

/// Projects one scope's settings, echoing the scope name the request used.
pub(crate) fn notify_settings_dto(
    scope: &'static str,
    settings: &tl::enums::PeerNotifySettings,
) -> NotifySettingsDto {
    NotifySettingsDto {
        scope,
        settings: peer_notify_settings_dto(settings),
    }
}

/// The layer reports dates in whole seconds; the wire format is epoch milliseconds.
fn millis(seconds: i32) -> i64 {
    i64::from(seconds) * 1_000
}

#[cfg(test)]
mod tests {
    //! Projection tests for the notification DTOs, built from layer values by hand so every field is
    //! pinned without a live session.

    use super::*;
    use serde_json::json;

    /// Asserts that [dto] encodes to exactly [expected].
    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    /// The empty settings: no flag is set, so nothing is claimed about any of them.
    fn settings() -> tl::enums::PeerNotifySettings {
        tl::enums::PeerNotifySettings::Settings(tl::types::PeerNotifySettings {
            show_previews: None,
            silent: None,
            mute_until: None,
            ios_sound: None,
            android_sound: None,
            other_sound: None,
            stories_muted: None,
            stories_hide_sender: None,
            stories_ios_sound: None,
            stories_android_sound: None,
            stories_other_sound: None,
        })
    }

    #[test]
    fn every_sound_constructor_maps_to_its_kind() {
        assert_json(
            &notification_sound_dto(&tl::enums::NotificationSound::Default),
            json!({ "kind": "default", "id": null, "title": null, "data": null }),
        );
        assert_json(
            &notification_sound_dto(&tl::enums::NotificationSound::None),
            json!({ "kind": "none", "id": null, "title": null, "data": null }),
        );
        assert_json(
            &notification_sound_dto(&tl::enums::NotificationSound::Local(
                tl::types::NotificationSoundLocal {
                    title: "Ping".to_owned(),
                    data: "base64-blob".to_owned(),
                }
            )),
            json!({ "kind": "local", "id": null, "title": "Ping", "data": "base64-blob" }),
        );
        assert_json(
            &notification_sound_dto(&tl::enums::NotificationSound::Ringtone(
                tl::types::NotificationSoundRingtone { id: 5150 }
            )),
            json!({ "kind": "ringtone", "id": 5150, "title": null, "data": null }),
        );
    }

    #[test]
    fn settings_with_nothing_set_stay_null_everywhere() {
        assert_json(
            &peer_notify_settings_dto(&settings()),
            json!({
                "showPreviews": null,
                "silent": null,
                "muteUntil": null,
                "iosSound": null,
                "androidSound": null,
                "otherSound": null,
                "storiesMuted": null,
                "storiesHideSender": null,
                "storiesIosSound": null,
                "storiesAndroidSound": null,
                "storiesOtherSound": null,
            }),
        );
    }

    #[test]
    fn the_settings_carry_the_flags_the_sounds_and_the_story_state() {
        let tl::enums::PeerNotifySettings::Settings(mut settings) = settings();
        settings.show_previews = Some(false);
        settings.silent = Some(true);
        settings.mute_until = Some(1_700_000_000);
        settings.android_sound = Some(tl::enums::NotificationSound::Default);
        settings.other_sound = Some(tl::enums::NotificationSound::None);
        settings.stories_muted = Some(true);
        settings.stories_hide_sender = Some(false);
        let dto = peer_notify_settings_dto(&tl::enums::PeerNotifySettings::Settings(settings));

        assert_eq!(dto.show_previews, Some(false));
        assert_eq!(dto.silent, Some(true));
        assert_eq!(dto.mute_until, Some(1_700_000_000_000));
        assert_eq!(dto.android_sound.expect("an android sound").kind, "default");
        assert_eq!(dto.other_sound.expect("an other sound").kind, "none");
        assert_eq!(dto.stories_muted, Some(true));
        assert_eq!(dto.stories_hide_sender, Some(false));
        assert!(dto.ios_sound.is_none());
    }

    #[test]
    fn the_answer_echoes_the_scope_it_was_asked_for() {
        assert_json(
            &notify_settings_dto("account", &settings()),
            json!({
                "scope": "account",
                "settings": {
                    "showPreviews": null,
                    "silent": null,
                    "muteUntil": null,
                    "iosSound": null,
                    "androidSound": null,
                    "otherSound": null,
                    "storiesMuted": null,
                    "storiesHideSender": null,
                    "storiesIosSound": null,
                    "storiesAndroidSound": null,
                    "storiesOtherSound": null,
                },
            }),
        );
    }

    #[test]
    fn a_peer_scope_is_echoed_just_as_plainly() {
        assert_eq!(notify_settings_dto("peer", &settings()).scope, "peer");
    }

    #[test]
    fn the_layer_seconds_project_into_milliseconds() {
        assert_eq!(millis(1_700_000_000), 1_700_000_000_000);
        assert_eq!(millis(0), 0);
    }
}