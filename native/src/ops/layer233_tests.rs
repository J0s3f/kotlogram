//! Tests for the layer-233 facade (`docs/layer-233-facade.md`), written before the handlers.
//!
//! Handlers need a live session, so what is pinned here is the pure part of each: the payload shape
//! Kotlin sends, the exact `tl::functions` request a payload builds, the validation that happens
//! before any request, and the projection of every result and update.

use grammers_client::tl;
use serde_json::json;

use super::{chats, files, inline, widgets};
use crate::dto::update::typed_layer233_update;
use crate::payload::PeerTarget;

fn decode<T: for<'de> serde::Deserialize<'de>>(value: serde_json::Value) -> T {
    serde_json::from_value(value).expect("the payload decodes")
}

fn self_peer() -> tl::enums::InputPeer {
    tl::enums::InputPeer::PeerSelf
}

fn channel() -> tl::enums::InputChannel {
    tl::enums::InputChannel::Channel(tl::types::InputChannel {
        channel_id: 5,
        access_hash: 6,
    })
}

// ---- forum sidebar --------------------------------------------------------------------------

#[test]
fn toggle_forum_threads_the_sidebar_flag_into_the_request() {
    let data: chats::ToggleForumPayload = decode(json!({
        "channel": {"peerHandle": 5}, "enabled": true, "tabs": true, "sidebar": true
    }));
    let request = chats::toggle_forum_request(channel(), &data);
    assert!(request.enabled && request.tabs && request.sidebar);
}

#[test]
fn toggle_forum_defaults_tabs_and_sidebar_to_off() {
    let data: chats::ToggleForumPayload =
        decode(json!({"channel": {"peerHandle": 5}, "enabled": true}));
    let request = chats::toggle_forum_request(channel(), &data);
    assert!(request.enabled);
    assert!(!request.tabs && !request.sidebar);
}

// ---- transcription --------------------------------------------------------------------------

#[test]
fn transcription_payloads_read_their_fields() {
    let file: files::TranscribeAudioFilePayload = decode(json!({"uploadHandle": 9}));
    assert_eq!(file.upload_handle, 9);
    let langs: files::GetTranscriptionLangsPayload = decode(json!({}));
    assert!(langs.peer.is_none());
    let langs: files::GetTranscriptionLangsPayload = decode(json!({"peer": {"peerHandle": 3}}));
    assert_eq!(langs.peer.map(|peer: PeerTarget| peer.peer_handle), Some(Some(3)));
}

#[test]
fn a_transcription_projects_its_trial_allowance_in_milliseconds() {
    let audio = tl::enums::messages::TranscribedAudio::Audio(tl::types::messages::TranscribedAudio {
        pending: true,
        transcription_id: 77,
        text: "he".to_owned(),
        trial_remains_num: Some(2),
        trial_remains_until_date: Some(1_700_000_000),
    });
    assert_eq!(
        serde_json::to_value(files::transcription_dto(&audio)).unwrap(),
        json!({
            "transcriptionId": 77, "text": "he", "pending": true,
            "trialRemaining": 2, "trialRemainingUntilDate": 1_700_000_000_000_i64
        })
    );
}

#[test]
fn a_final_transcription_has_no_trial_fields() {
    let audio = tl::enums::messages::TranscribedAudio::Audio(tl::types::messages::TranscribedAudio {
        pending: false,
        transcription_id: 1,
        text: "done".to_owned(),
        trial_remains_num: None,
        trial_remains_until_date: None,
    });
    let value = serde_json::to_value(files::transcription_dto(&audio)).unwrap();
    assert_eq!(value["pending"], json!(false));
    assert_eq!(value["trialRemaining"], json!(null));
    assert_eq!(value["trialRemainingUntilDate"], json!(null));
}

// ---- widget media ---------------------------------------------------------------------------

fn spec(html: &str, width: i32, height: i32) -> widgets::WidgetSpec {
    decode(json!({"html": html, "width": width, "height": height}))
}

#[test]
fn a_widget_spec_builds_the_layers_widget_media() {
    let spec: widgets::WidgetSpec = decode(json!({
        "html": "<b>x</b>", "width": 320, "height": 200,
        "allowQueries": true, "sharedWidget": true, "publicData": "p", "chatData": "c"
    }));
    match widgets::widget_input_media(Some(&spec), None, None).expect("a valid widget") {
        tl::enums::InputMedia::Widget(media) => {
            assert_eq!((media.width, media.height), (320, 200));
            assert_eq!(media.html, "<b>x</b>");
            assert!(media.allow_queries && media.shared_widget);
            assert_eq!(media.public_data.as_deref(), Some("p"));
            assert_eq!(media.chat_data.as_deref(), Some("c"));
        }
        other => panic!("expected inputMediaWidget, got {other:?}"),
    }
}

#[test]
fn an_existing_widget_is_sent_by_id_with_per_chat_data() {
    match widgets::widget_input_media(None, Some(99), Some("c")).expect("a widget id") {
        tl::enums::InputMedia::WidgetId(media) => {
            assert_eq!(media.widget_id, 99);
            assert_eq!(media.chat_data.as_deref(), Some("c"));
        }
        other => panic!("expected inputMediaWidgetId, got {other:?}"),
    }
}

#[test]
fn a_widget_needs_exactly_one_of_a_spec_or_an_id() {
    assert!(widgets::widget_input_media(None, None, None).is_err());
    let spec = spec("<i/>", 1, 1);
    assert!(widgets::widget_input_media(Some(&spec), Some(1), None).is_err());
}

#[test]
fn a_widget_must_have_html_and_a_positive_size() {
    assert!(widgets::widget_input_media(Some(&spec("", 10, 10)), None, None).is_err());
    assert!(widgets::widget_input_media(Some(&spec("<i/>", 0, 10)), None, None).is_err());
    assert!(widgets::widget_input_media(Some(&spec("<i/>", 10, -1)), None, None).is_err());
}

#[test]
fn a_widget_send_is_a_send_media_with_the_caption_and_send_options() {
    let data: widgets::SendWidgetPayload = decode(json!({
        "peer": {"peerHandle": 4}, "widget": {"html": "<i/>", "width": 10, "height": 20},
        "caption": "look", "silent": true, "replyToMessageId": 8, "scheduleDate": 1_700_000_000_000_i64
    }));
    let request = widgets::send_widget_request(self_peer(), &data, 4242).expect("a valid send");
    assert_eq!(request.message, "look");
    assert!(request.silent);
    assert_eq!(request.random_id, 4242);
    assert_eq!(request.schedule_date, Some(1_700_000_000));
    assert!(request.reply_to.is_some());
    assert!(matches!(request.media, tl::enums::InputMedia::Widget(_)));
}

// ---- widget data ----------------------------------------------------------------------------

#[test]
fn public_data_is_requested_with_the_cached_hash() {
    let data: widgets::GetWidgetPublicDataPayload =
        decode(json!({"peer": {"peerHandle": 4}, "messageId": 12, "hash": 5}));
    let request = widgets::get_public_data_request(self_peer(), &data);
    assert_eq!((request.msg_id, request.hash), (12, 5));
    let first: widgets::GetWidgetPublicDataPayload =
        decode(json!({"peer": {"peerHandle": 4}, "messageId": 12}));
    assert_eq!(widgets::get_public_data_request(self_peer(), &first).hash, 0);
}

#[test]
fn public_data_projects_as_modified_or_not_modified() {
    let fresh = tl::enums::messages::WidgetPublicData::Data(tl::types::messages::WidgetPublicData {
        hash: 5,
        data: "{}".to_owned(),
        period: 60,
    });
    assert_eq!(
        serde_json::to_value(widgets::public_data_dto(&fresh)).unwrap(),
        json!({"notModified": false, "hash": 5, "data": "{}", "periodSeconds": 60})
    );
    let same = tl::enums::messages::WidgetPublicData::NotModified(
        tl::types::messages::WidgetPublicDataNotModified { period: 30 },
    );
    assert_eq!(
        serde_json::to_value(widgets::public_data_dto(&same)).unwrap(),
        json!({"notModified": true, "hash": null, "data": null, "periodSeconds": 30})
    );
}

#[test]
fn widget_data_edits_send_only_the_fields_given() {
    let data: widgets::EditMessageWidgetDataPayload =
        decode(json!({"peer": {"peerHandle": 4}, "messageId": 12, "publicData": "p"}));
    let request = widgets::edit_message_data_request(self_peer(), &data);
    assert_eq!(request.id, 12);
    assert_eq!(request.public_data.as_deref(), Some("p"));
    assert_eq!(request.chat_data, None);

    let inline: widgets::EditInlineMessageWidgetDataPayload = decode(json!({
        "messageId": {"dcId": 2, "accessHash": 3, "id": 4}, "chatData": "c"
    }));
    let request = widgets::edit_inline_data_request(&inline);
    assert_eq!(request.public_data, None);
    assert_eq!(request.chat_data.as_deref(), Some("c"));

    let widget: widgets::EditWidgetDataPayload =
        decode(json!({"widgetId": 8, "publicData": "p", "cacheTimeSeconds": 60}));
    let request = widgets::edit_widget_data_request(&widget);
    assert_eq!((request.widget_id, request.cache_time), (8, Some(60)));
    assert_eq!(request.public_data, "p");
}

#[test]
fn a_widget_query_answer_request_decodes_its_data_and_wallet_flag() {
    let data: widgets::GetBotWidgetQueryAnswerPayload = decode(json!({
        "peer": {"peerHandle": 4}, "messageId": 12, "dataBase64": "AQID", "withWallet": true
    }));
    let request = widgets::get_answer_request(self_peer(), &data).expect("valid base64");
    assert_eq!(request.data, vec![1, 2, 3]);
    match request.query_type {
        tl::enums::WidgetQueryType::Data(kind) => assert!(kind.with_wallet),
        other => panic!("expected widgetQueryTypeData, got {other:?}"),
    }
    let bad: widgets::GetBotWidgetQueryAnswerPayload =
        decode(json!({"peer": {"peerHandle": 4}, "messageId": 12, "dataBase64": "!!"}));
    assert!(widgets::get_answer_request(self_peer(), &bad).is_err());

    let set: widgets::SetBotWidgetQueryAnswerPayload = decode(json!({"queryId": 6, "result": "r"}));
    let request = widgets::set_answer_request(&set);
    assert_eq!((request.query_id, request.result.as_deref()), (6, Some("r")));
}

// ---- inline widget result -------------------------------------------------------------------

#[test]
fn an_inline_widget_result_wraps_the_widget_message() {
    let result_spec: inline::InlineResultSpec = decode(json!({
        "kind": "widget", "id": "w1", "title": "Widget", "messageText": "caption",
        "widget": {"html": "<i/>", "width": 10, "height": 20}
    }));
    match inline::inline_result(&result_spec).expect("a valid widget result") {
        tl::enums::InputBotInlineResult::Result(result) => {
            assert_eq!(result.id, "w1");
            assert_eq!(result.title.as_deref(), Some("Widget"));
            match result.send_message {
                tl::enums::InputBotInlineMessage::Widget(message) => {
                    assert_eq!(message.message, "caption");
                    assert_eq!((message.width, message.height), (10, 20));
                }
                other => panic!("expected inputBotInlineMessageWidget, got {other:?}"),
            }
        }
        other => panic!("expected a plain inline result, got {other:?}"),
    }
}

#[test]
fn an_inline_widget_result_by_id_uses_the_id_message() {
    let result_spec: inline::InlineResultSpec = decode(json!({
        "kind": "widget", "id": "w2", "title": "Again", "widgetId": 99
    }));
    match inline::inline_result(&result_spec).expect("a valid widget result") {
        tl::enums::InputBotInlineResult::Result(result) => assert!(matches!(
            result.send_message,
            tl::enums::InputBotInlineMessage::WidgetId(_)
        )),
        other => panic!("expected a plain inline result, got {other:?}"),
    }
}

#[test]
fn an_inline_widget_result_without_a_widget_is_refused() {
    let result_spec: inline::InlineResultSpec =
        decode(json!({"kind": "widget", "id": "w3", "title": "Empty"}));
    assert!(inline::inline_result(&result_spec).is_err());
}

// ---- updates --------------------------------------------------------------------------------

fn user_peer(id: i64) -> tl::enums::Peer {
    tl::enums::Peer::User(tl::types::PeerUser { user_id: id })
}

#[test]
fn a_forum_topic_edit_projects_only_what_changed() {
    let update = tl::enums::Update::EditForumTopic(tl::types::UpdateEditForumTopic {
        peer: tl::enums::Peer::Channel(tl::types::PeerChannel { channel_id: 5 }),
        topic_id: 7,
        title: Some("New".to_owned()),
        icon_emoji_id: None,
        closed: Some(true),
        hidden: None,
    });
    let dto = typed_layer233_update(&update).expect("a typed update");
    assert_eq!(dto.kind, "forumTopicEdited");
    assert_eq!(
        serde_json::to_value(dto.forum_topic_edit.expect("projected")).unwrap(),
        json!({
            "peerId": 5, "peerKind": "channel", "topicId": 7, "title": "New",
            "iconEmojiId": null, "closed": true, "hidden": null
        })
    );
    assert!(dto.raw_update.is_some(), "the raw bytes are kept");
}

#[test]
fn a_bot_widget_query_names_its_message() {
    let update = tl::enums::Update::BotWidgetQuery(tl::types::UpdateBotWidgetQuery {
        query_id: 1,
        query_type: tl::enums::WidgetQueryType::Data(tl::types::WidgetQueryTypeData {
            with_wallet: false,
        }),
        user_id: 2,
        peer: user_peer(5),
        msg_id: 9,
        chat_instance: 3,
        chat_type: "sender".to_owned(),
        data: vec![1, 2, 3],
        widget_id: Some(4),
        wallet_card: None,
    });
    let dto = typed_layer233_update(&update).expect("a typed update");
    assert_eq!(dto.kind, "botWidgetQuery");
    let value = serde_json::to_value(dto.widget_query.expect("projected")).unwrap();
    assert_eq!(value["queryType"], json!("data"));
    assert_eq!(value["peerId"], json!(5));
    assert_eq!(value["peerKind"], json!("user"));
    assert_eq!(value["messageId"], json!(9));
    assert_eq!(value["inlineMessageId"], json!(null));
    assert_eq!(value["dataBase64"], json!("AQID"));
    assert_eq!(value["widgetId"], json!(4));
}

#[test]
fn an_inline_bot_widget_query_names_its_inline_message() {
    let update = tl::enums::Update::InlineBotWidgetQuery(tl::types::UpdateInlineBotWidgetQuery {
        query_id: 1,
        query_type: tl::enums::WidgetQueryType::SentTransaction(
            tl::types::WidgetQueryTypeSentTransaction {
                wallet: tl::enums::UserWallet::Wallet(tl::types::UserWallet {
                    address: "a".to_owned(),
                    balance: 0,
                    public_key: None,
                    state_init: None,
                }),
                msg_hash: "h".to_owned(),
            },
        ),
        user_id: 2,
        inline_message_id: tl::enums::InputBotInlineMessageId::Id(tl::types::InputBotInlineMessageId {
            dc_id: 2,
            id: 4,
            access_hash: 3,
        }),
        chat_instance: 3,
        chat_type: "sender".to_owned(),
        data: Vec::new(),
        widget_id: None,
        wallet_card: None,
    });
    let dto = typed_layer233_update(&update).expect("a typed update");
    assert_eq!(dto.kind, "inlineBotWidgetQuery");
    let value = serde_json::to_value(dto.widget_query.expect("projected")).unwrap();
    assert_eq!(value["queryType"], json!("sentTransaction"));
    assert_eq!(value["messageId"], json!(null));
    assert_eq!(value["inlineMessageId"], json!({"dcId": 2, "accessHash": 3, "id": 4}));
}

#[test]
fn a_message_widget_data_update_and_a_transcription_project() {
    let data = tl::enums::Update::MessageWidgetData(tl::types::UpdateMessageWidgetData {
        peer: user_peer(5),
        msg_id: 9,
        public_data: Some("p".to_owned()),
        public_data_hash: Some(11),
        chat_data: None,
        pts: 1,
        pts_count: 1,
    });
    let dto = typed_layer233_update(&data).expect("a typed update");
    assert_eq!(dto.kind, "messageWidgetData");
    assert_eq!(
        serde_json::to_value(dto.widget_data.expect("projected")).unwrap(),
        json!({
            "peerId": 5, "peerKind": "user", "messageId": 9,
            "publicData": "p", "publicDataHash": 11, "chatData": null
        })
    );

    let transcribed = tl::enums::Update::TranscribedAudioFile(tl::types::UpdateTranscribedAudioFile {
        pending: true,
        transcription_id: 5,
        text: "t".to_owned(),
    });
    let dto = typed_layer233_update(&transcribed).expect("a typed update");
    assert_eq!(dto.kind, "audioTranscribed");
    assert_eq!(
        serde_json::to_value(dto.transcription.expect("projected")).unwrap(),
        json!({"transcriptionId": 5, "text": "t", "pending": true})
    );
}

#[test]
fn an_unrelated_update_is_left_to_the_raw_path() {
    let update = tl::enums::Update::DeleteMessages(tl::types::UpdateDeleteMessages {
        messages: vec![1],
        pts: 1,
        pts_count: 1,
    });
    assert!(typed_layer233_update(&update).is_none());
}

// ---- routing --------------------------------------------------------------------------------

#[test]
fn every_new_operation_routes_to_its_own_module() {
    for (name, route) in [
        ("toggleForum", chats::route("toggleForum")),
        ("transcribeAudioFile", files::route("transcribeAudioFile")),
        ("getTranscriptionLangs", files::route("getTranscriptionLangs")),
        ("sendWidget", widgets::route("sendWidget")),
        ("getWidgetPublicData", widgets::route("getWidgetPublicData")),
        ("editMessageWidgetData", widgets::route("editMessageWidgetData")),
        ("editInlineMessageWidgetData", widgets::route("editInlineMessageWidgetData")),
        ("editWidgetData", widgets::route("editWidgetData")),
        ("getBotWidgetQueryAnswer", widgets::route("getBotWidgetQueryAnswer")),
        ("setBotWidgetQueryAnswer", widgets::route("setBotWidgetQueryAnswer")),
    ] {
        assert!(route.is_some(), "{name} is not routed");
    }
    assert!(widgets::route("toggleForum").is_none());
}
