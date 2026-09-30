//! Wire-contract tests for the JSON projections.
//!
//! These are the only place a projection is built by hand, because the operations can only reach
//! one through a live Telegram session. Each test pins the exact document: renaming a field, or
//! changing the millisecond timestamps to seconds, breaks a test here rather than a Kotlin
//! decoder.

use grammers_client::tl;
use serde::Serialize;
use serde_json::json;

use crate::dto::action::MessageActionDto;
use crate::dto::dialog::DialogDto;
use crate::dto::markup::{ButtonDto, ReplyMarkupDto};
use crate::dto::media::{document_kind, MediaDto};
use crate::dto::message::{ForwardHeaderDto, MessageDto, ReplyHeaderDto};
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
            ..ChatPermissionsDto::default()
        }),
    }
}

fn full_forward_header() -> ForwardHeaderDto {
    ForwardHeaderDto {
        imported: true,
        saved_out: true,
        from_id: Some(7),
        from_name: Some("Some One".to_owned()),
        date: 1_700_000_000_000,
        channel_post: Some(42),
        post_author: Some("Author".to_owned()),
        saved_from_peer: Some(-1_000_007),
        saved_from_msg_id: Some(30),
        saved_from_id: Some(8),
        saved_from_name: Some("Original".to_owned()),
        saved_date: Some(1_700_000_060_000),
        psa_type: Some("psa_type".to_owned()),
    }
}

fn full_reply_header() -> ReplyHeaderDto {
    ReplyHeaderDto {
        kind: "header",
        reply_to_scheduled: true,
        forum_topic: true,
        quote: true,
        reply_to_ephemeral: true,
        reply_to_msg_id: Some(30),
        reply_to_peer_id: Some(-1_000_007),
        reply_from: Some(full_forward_header()),
        reply_media: None,
        reply_to_top_id: Some(29),
        quote_text: Some("quoted".to_owned()),
        quote_offset: Some(3),
        todo_item_id: Some(5),
        poll_option: Some("AQID".to_owned()),
        story_peer: None,
        story_id: None,
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
        forward_header: Some(full_forward_header()),
        reply_header: Some(full_reply_header()),
        restriction_reasons: vec![RestrictionReasonDto {
            platforms: vec!["all".to_owned(), "ios".to_owned()],
            reason: "spam".to_owned(),
            text: "Reported as spam".to_owned(),
        }],
        action: Some(MessageActionDto {
            message_id: 31,
            sender_id: Some(7),
            kind: "pinMessage",
        }),
        reply_markup: Some(ReplyMarkupDto {
            kind: "inline",
            rows: vec![vec![ButtonDto {
                kind: "url",
                text: "Open".to_owned(),
                url: Some("https://example.org".to_owned()),
                data: None,
                requires_password: None,
                fwd_text: None,
                button_id: None,
                query: None,
                same_peer: None,
                peer_types: None,
                quiz: None,
                user_id: None,
                copy_text: None,
                max_quantity: None,
                request_write_access: None,
            }]],
            fit_size: false,
            single_use: false,
            selective: false,
            persistent: false,
            placeholder: None,
        }),
        peer: Some(full_peer()),
        sender: Some(full_user()),
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
            ..ChatPermissionsDto::default()
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
                "manageTopics": false,
                "postStories": false,
                "editStories": false,
                "deleteStories": false,
                "manageDirectMessages": false,
                "manageRanks": false,
                "manageLinkedPeers": false,
                "manageWelcomeMessages": false,
                "other": false,
            },
        }),
    );
}

#[test]
fn message_encodes_every_projected_field() {
    let mut expected = json!({
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
    });
    let object = expected.as_object_mut().expect("an object");
    object.insert(
        "forwardHeader".to_owned(),
        serde_json::to_value(full_forward_header()).expect("a forward header encodes"),
    );
    object.insert(
        "replyHeader".to_owned(),
        serde_json::to_value(full_reply_header()).expect("a reply header encodes"),
    );
    object.insert(
        "restrictionReasons".to_owned(),
        serde_json::to_value(vec![RestrictionReasonDto {
            platforms: vec!["all".to_owned(), "ios".to_owned()],
            reason: "spam".to_owned(),
            text: "Reported as spam".to_owned(),
        }])
        .expect("restriction reasons encode"),
    );
    object.insert(
        "action".to_owned(),
        serde_json::to_value(MessageActionDto {
            message_id: 31,
            sender_id: Some(7),
            kind: "pinMessage",
        })
        .expect("an action encodes"),
    );
    object.insert(
        "replyMarkup".to_owned(),
        serde_json::to_value(ReplyMarkupDto {
            kind: "inline",
            rows: vec![vec![ButtonDto {
                kind: "url",
                text: "Open".to_owned(),
                url: Some("https://example.org".to_owned()),
                data: None,
                requires_password: None,
                fwd_text: None,
                button_id: None,
                query: None,
                same_peer: None,
                peer_types: None,
                quiz: None,
                user_id: None,
                copy_text: None,
                max_quantity: None,
                request_write_access: None,
            }]],
            fit_size: false,
            single_use: false,
            selective: false,
            persistent: false,
            placeholder: None,
        })
        .expect("a markup encodes"),
    );
    object.insert("peer".to_owned(), json!(full_peer()));
    object.insert("sender".to_owned(), json!(full_user()));
    assert_json(&full_message(None), expected);
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
            "manageTopics": false,
            "postStories": false,
            "editStories": false,
            "deleteStories": false,
            "manageDirectMessages": false,
            "manageRanks": false,
            "manageLinkedPeers": false,
            "manageWelcomeMessages": false,
            "other": false,
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
    // `full_media` is a `application/pdf` document: no other kind names it.
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
fn a_document_is_named_by_what_it_carries() {
    // The layer calls all of these `messageMediaDocument`; the MIME type and the animation flag
    // are what tell them apart.
    assert_eq!(document_kind(false, Some("video/mp4")), "video");
    assert_eq!(document_kind(false, Some("audio/ogg")), "audio");
    assert_eq!(document_kind(true, Some("video/mp4")), "animation");
    // An animation is named by its flag, whatever its MIME type says.
    assert_eq!(document_kind(true, Some("image/gif")), "animation");
    // A plain file, and a document with no MIME type at all, stay documents.
    assert_eq!(document_kind(false, Some("application/pdf")), "document");
    assert_eq!(document_kind(false, None), "document");
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
fn admin_rights_project_every_layer_flag() {
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
        manage_ranks: false,
        manage_linked_peers: false,
        manage_welcome_messages: false,
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
            "manageTopics": true,
            "postStories": true,
            "editStories": true,
            "deleteStories": true,
            "manageDirectMessages": true,
            "manageRanks": false,
            "manageLinkedPeers": false,
            "manageWelcomeMessages": false,
            "other": true,
        }),
    );
}

#[test]
fn banned_rights_project_every_layer_flag() {
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
        edit_rank: false,
        send_reactions: false,
        manage_linked_peers: false,
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
            "manageTopics": true,
            "sendPhotos": true,
            "sendVideos": true,
            "sendRoundvideos": true,
            "sendAudios": true,
            "sendVoices": true,
            "sendDocs": true,
            "sendPlain": true,
            "editRank": false,
            "sendReactions": false,
            "manageLinkedPeers": false,
            "untilDate": 1_700_000_000_000i64,
        }),
    );
}
