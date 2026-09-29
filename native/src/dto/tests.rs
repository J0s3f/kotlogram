//! Wire-contract tests for the JSON projections.
//!
//! These are the only place a projection is built by hand, because the operations can only reach
//! one through a live Telegram session. Each test pins the exact document: renaming a field, or
//! changing the millisecond timestamps to seconds, breaks a test here rather than a Kotlin
//! decoder.

use grammers_client::grammers_tl_types as tl;
use serde::Serialize;
use serde_json::json;

use crate::dto::dialog::DialogDto;
use crate::dto::media::MediaDto;
use crate::dto::message::MessageDto;
use crate::dto::participant::ParticipantDto;
use crate::dto::peer::PeerDto;
use crate::dto::permissions::{ChatPermissionsDto, ChatRestrictionsDto};
use crate::dto::user::{RestrictionReasonDto, UserDto};

/// Asserts that [dto] encodes to exactly [expected], and that the text it produced parses back
/// into the same document.
fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
    let text = serde_json::to_string(dto).expect("a projection always encodes");
    let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
    assert_eq!(actual, expected);
}

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
        is_bot: true,
        bot_privacy: true,
        bot_supports_chats: false,
        bot_inline_geo: false,
        bot_inline_placeholder: Some("pick a bot".to_owned()),
        verified: true,
        restricted: true,
        support: false,
        scam: false,
        restriction_reasons: vec![RestrictionReasonDto {
            platforms: vec!["all".to_owned(), "ios".to_owned()],
            reason: "spam".to_owned(),
            text: "Reported as spam".to_owned(),
        }],
    }
}

fn full_peer() -> PeerDto {
    PeerDto {
        native_handle: 12,
        id: -1_000_007,
        kind: "channel",
        username: Some("channel".to_owned()),
        name: Some("A Channel".to_owned()),
        usernames: vec!["channel_alt".to_owned()],
        is_megagroup: None,
        has_photo: true,
        permissions: Some(ChatPermissionsDto {
            change_info: true,
            post_messages: true,
            edit_messages: false,
            delete_messages: false,
            ban_users: false,
            invite_users: false,
            pin_messages: false,
            add_admins: false,
            anonymous: false,
            manage_call: false,
        }),
    }
}

fn full_message(media: Option<MediaDto>) -> MessageDto {
    MessageDto {
        id: 31,
        text: "hello".to_owned(),
        outgoing: true,
        reply_to_message_id: Some(30),
        peer_id: Some(-1_000_007),
        sender_id: Some(7),
        date: 1_700_000_000_000,
        edit_date: Some(1_700_000_060_000),
        mentioned: true,
        media_unread: false,
        silent: true,
        pinned: true,
        from_channel_post: false,
        from_scheduled: true,
        edit_hide: false,
        via_bot_id: Some(99),
        post_author: Some("Author".to_owned()),
        grouped_id: Some(88),
        view_count: Some(5),
        forward_count: Some(4),
        reply_count: Some(3),
        reaction_count: Some(2),
        media,
    }
}

fn full_dialog() -> DialogDto {
    DialogDto {
        peer: full_peer(),
        last_message: Some(full_message(None)),
        pinned: true,
        top_message: 31,
        unread_count: Some(2),
        unread_mentions_count: Some(1),
        draft_text: Some("unsent".to_owned()),
        folder_id: Some(1),
        is_folder: false,
    }
}

fn full_participant() -> ParticipantDto {
    ParticipantDto {
        user: full_user(),
        role: "admin",
        rank: Some("Owner".to_owned()),
        date: Some(1_700_000_000_000),
        invited_by: Some(7),
        promoted_by: Some(8),
        kicked_by: None,
        can_edit: Some(true),
        left: None,
        permissions: Some(ChatPermissionsDto {
            change_info: true,
            post_messages: false,
            edit_messages: true,
            delete_messages: true,
            ban_users: true,
            invite_users: true,
            pin_messages: true,
            add_admins: true,
            anonymous: false,
            manage_call: true,
        }),
        restrictions: None,
    }
}

fn full_media() -> MediaDto {
    let mut media = MediaDto::empty("document");
    media.id = Some(5_150);
    media.size = Some(12_345);
    media.name = Some("report.pdf".to_owned());
    media.mime_type = Some("application/pdf".to_owned());
    media.creation_date = Some(1_700_000_000_000);
    media.spoiler = Some(false);
    media.is_animated = Some(false);
    media
}

/// Every field a [MediaDto] can emit, which the tests below compare the encoding against. The
/// list is deliberately explicit: adding a field to the media projection means adding it here and
/// to the Kotlin mirror, not silently changing the wire shape.
const MEDIA_FIELDS: &[&str] = &[
    "accuracyRadius",
    "address",
    "author",
    "audioTitle",
    "closed",
    "creationDate",
    "description",
    "displayUrl",
    "duration",
    "emoji",
    "firstName",
    "heading",
    "height",
    "id",
    "isAnimated",
    "isQuiz",
    "kind",
    "latitude",
    "lastName",
    "longitude",
    "mimeType",
    "name",
    "pageType",
    "performer",
    "period",
    "phoneNumber",
    "provider",
    "proximityNotificationRadius",
    "question",
    "resolutionHeight",
    "resolutionWidth",
    "siteName",
    "size",
    "spoiler",
    "title",
    "totalVoters",
    "ttlSeconds",
    "url",
    "value",
    "venueId",
    "venueType",
    "vcard",
    "width",
];

/// The names a [MediaDto] actually encoded, sorted, or `None` if it did not encode as an object.
fn media_field_names(media: &MediaDto) -> Option<Vec<String>> {
    let mut names: Vec<String> = serde_json::to_value(media)
        .ok()?
        .as_object()?
        .keys()
        .cloned()
        .collect();
    names.sort();
    Some(names)
}

#[test]
fn user_encodes_every_projected_field() {
    assert_json(
        &full_user(),
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
            "isBot": true,
            "botPrivacy": true,
            "botSupportsChats": false,
            "botInlineGeo": false,
            "botInlinePlaceholder": "pick a bot",
            "verified": true,
            "restricted": true,
            "support": false,
            "scam": false,
            "restrictionReasons": [{
                "platforms": ["all", "ios"],
                "reason": "spam",
                "text": "Reported as spam",
            }],
        }),
    );
}

#[test]
fn peer_encodes_every_projected_field() {
    assert_json(
        &full_peer(),
        json!({
            "nativeHandle": 12,
            "id": -1_000_007,
            "kind": "channel",
            "username": "channel",
            "name": "A Channel",
            "usernames": ["channel_alt"],
            "isMegagroup": null,
            "hasPhoto": true,
            "permissions": {
                "changeInfo": true,
                "postMessages": true,
                "editMessages": false,
                "deleteMessages": false,
                "banUsers": false,
                "inviteUsers": false,
                "pinMessages": false,
                "addAdmins": false,
                "anonymous": false,
                "manageCall": false,
            },
        }),
    );
}

#[test]
fn message_encodes_every_projected_field() {
    assert_json(
        &full_message(None),
        json!({
            "id": 31,
            "text": "hello",
            "outgoing": true,
            "replyToMessageId": 30,
            "peerId": -1_000_007,
            "senderId": 7,
            "date": 1_700_000_000_000i64,
            "editDate": 1_700_000_060_000i64,
            "mentioned": true,
            "mediaUnread": false,
            "silent": true,
            "pinned": true,
            "fromChannelPost": false,
            "fromScheduled": true,
            "editHide": false,
            "viaBotId": 99,
            "postAuthor": "Author",
            "groupedId": 88,
            "viewCount": 5,
            "forwardCount": 4,
            "replyCount": 3,
            "reactionCount": 2,
            "media": null,
        }),
    );
}

#[test]
fn dialog_encodes_every_projected_field() {
    let mut expected = json!({
        "pinned": true,
        "topMessage": 31,
        "unreadCount": 2,
        "unreadMentionsCount": 1,
        "draftText": "unsent",
        "folderId": 1,
        "isFolder": false,
    });
    let object = expected.as_object_mut().expect("an object");
    object.insert("peer".to_owned(), json!(full_peer()));
    let last_message = full_message(None);
    object.insert(
        "lastMessage".to_owned(),
        serde_json::to_value(last_message).expect("a message encodes"),
    );
    assert_json(&full_dialog(), expected);
}

#[test]
fn participant_encodes_every_projected_field() {
    let mut expected = json!({
        "role": "admin",
        "rank": "Owner",
        "date": 1_700_000_000_000i64,
        "invitedBy": 7,
        "promotedBy": 8,
        "kickedBy": null,
        "canEdit": true,
        "left": null,
        "permissions": {
            "changeInfo": true,
            "postMessages": false,
            "editMessages": true,
            "deleteMessages": true,
            "banUsers": true,
            "inviteUsers": true,
            "pinMessages": true,
            "addAdmins": true,
            "anonymous": false,
            "manageCall": true,
        },
        "restrictions": null,
    });
    expected
        .as_object_mut()
        .expect("an object")
        .insert("user".to_owned(), json!(full_user()));
    assert_json(&full_participant(), expected);
}

#[test]
fn media_encodes_the_fields_of_its_kind() {
    let encoded = serde_json::to_value(full_media()).expect("media encodes");
    assert_eq!(encoded["kind"], json!("document"));

    // Every field is always present, so a kind is distinguished by which ones stop being null.
    let populated: serde_json::Map<String, serde_json::Value> = encoded
        .as_object()
        .expect("an object")
        .iter()
        .filter(|(_, value)| !value.is_null())
        .map(|(name, value)| (name.clone(), value.clone()))
        .collect();
    assert_eq!(
        serde_json::Value::Object(populated),
        json!({
            "kind": "document",
            "id": 5_150,
            "size": 12_345,
            "name": "report.pdf",
            "mimeType": "application/pdf",
            "creationDate": 1_700_000_000_000i64,
            "spoiler": false,
            "isAnimated": false,
        })
    );
}

#[test]
fn media_declares_exactly_the_projected_fields() {
    let mut expected: Vec<String> = MEDIA_FIELDS.iter().map(|name| (*name).to_owned()).collect();
    expected.sort();
    assert_eq!(
        media_field_names(&full_media()).as_deref(),
        Some(&expected[..])
    );
    assert_eq!(
        media_field_names(&MediaDto::empty("unknown")).as_deref(),
        Some(&expected[..])
    );
}

#[test]
fn unknown_media_still_carries_an_object() {
    let media = MediaDto::empty("unknown");
    let encoded = serde_json::to_value(&media).expect("media encodes");
    assert_eq!(encoded["kind"], json!("unknown"));
    assert_eq!(encoded["id"], json!(null));
    assert!(encoded.as_object().expect("an object").len() > 1);
}

#[test]
fn message_carries_its_media() {
    let encoded =
        serde_json::to_value(full_message(Some(full_media()))).expect("a message encodes");
    assert_eq!(encoded["media"]["kind"], json!("document"));
    assert_eq!(encoded["media"]["mimeType"], json!("application/pdf"));
}

#[test]
fn admin_rights_only_project_the_grammers_accessors() {
    let rights = tl::types::ChatAdminRights {
        change_info: true,
        post_messages: false,
        edit_messages: true,
        delete_messages: false,
        ban_users: true,
        invite_users: false,
        pin_messages: true,
        add_admins: false,
        anonymous: true,
        manage_call: false,
        other: true,
        manage_topics: true,
        post_stories: true,
        edit_stories: true,
        delete_stories: true,
        manage_direct_messages: true,
    };
    assert_json(
        &ChatPermissionsDto::from(&rights),
        json!({
            "changeInfo": true,
            "postMessages": false,
            "editMessages": true,
            "deleteMessages": false,
            "banUsers": true,
            "inviteUsers": false,
            "pinMessages": true,
            "addAdmins": false,
            "anonymous": true,
            "manageCall": false,
        }),
    );
}

#[test]
fn banned_rights_project_the_grammers_accessors() {
    let rights = tl::types::ChatBannedRights {
        view_messages: true,
        send_messages: false,
        send_media: true,
        send_stickers: false,
        send_gifs: true,
        send_games: false,
        send_inline: true,
        embed_links: false,
        send_polls: true,
        change_info: false,
        invite_users: true,
        pin_messages: false,
        manage_topics: true,
        send_photos: true,
        send_videos: true,
        send_roundvideos: true,
        send_audios: true,
        send_voices: true,
        send_docs: true,
        send_plain: true,
        until_date: 1_700_000_000,
    };
    assert_json(
        &ChatRestrictionsDto::from(&rights),
        json!({
            "viewMessages": true,
            "sendMessages": false,
            "sendMedia": true,
            "sendStickers": false,
            "sendGifs": true,
            "sendGames": false,
            "sendInline": true,
            "embedLinks": false,
            "sendPolls": true,
            "changeInfo": false,
            "inviteUsers": true,
            "pinMessages": false,
            "untilDate": 1_700_000_000_000i64,
        }),
    );
}
