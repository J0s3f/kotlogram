//! Service-action projection.
//!
//! grammers reports the service action of a message as a raw `grammers-tl-types` value
//! ([`ClientMessage::action`]): `MessageAction` is an enum of 58 layer variants with no accessors
//! at all, and several of them carry further layer types rather than plain fields. There is
//! therefore no per-variant accessor surface to mirror, and re-deriving the payload of all 58
//! variants here would be a second, raw-layer implementation of the schema rather than a
//! projection of grammers.
//!
//! What a caller can act on without that is *which* action a service message reports, which
//! message it belongs to and who caused it, so that is what this projection sends. Every variant of
//! the layer enum is named; none is reported as unknown.

use grammers_client::grammers_tl_types as tl;
use grammers_client::types::Message as ClientMessage;
use serde::Serialize;

/// The service action of a message, as far as grammers exposes it in a usable form.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct MessageActionDto {
    /// The message the action belongs to, echoed so a caller that only asked for the action can
    /// still address the message it came from.
    pub(crate) message_id: i32,
    /// The user who caused the service message, or `None` when grammers placed no sender.
    pub(crate) sender_id: Option<i64>,
    /// The action, as the lowerCamelCase name of the layer variant, for example `chatCreate` or
    /// `pinMessage`. This is the whole of the action: the layer's per-variant payload is left out
    /// for the reason given in the module docs.
    pub(crate) kind: &'static str,
}

/// Projects the service action of a message, or returns `None` for an ordinary message.
///
/// grammers only reports an action for a service message, so a `None` here means the message
/// carries no action at all rather than that the action could not be read.
pub(crate) fn message_action_dto(message: &ClientMessage) -> Option<MessageActionDto> {
    let action = message.action()?;
    Some(MessageActionDto {
        message_id: message.id(),
        // grammers derives the sender from the chat a message was fetched in, and an empty message
        // carries neither, so asking for it would panic rather than answer with none.
        sender_id: (!matches!(message.raw, tl::enums::Message::Empty(_)))
            .then(|| message.sender())
            .flatten()
            .map(|sender| sender.id().bot_api_dialog_id()),
        kind: action_kind(action),
    })
}

/// Names a service action after the layer variant it is.
fn action_kind(action: &tl::enums::MessageAction) -> &'static str {
    match action {
        tl::enums::MessageAction::Empty => "empty",
        tl::enums::MessageAction::ChatCreate(_) => "chatCreate",
        tl::enums::MessageAction::ChatEditTitle(_) => "chatEditTitle",
        tl::enums::MessageAction::ChatEditPhoto(_) => "chatEditPhoto",
        tl::enums::MessageAction::ChatDeletePhoto => "chatDeletePhoto",
        tl::enums::MessageAction::ChatAddUser(_) => "chatAddUser",
        tl::enums::MessageAction::ChatDeleteUser(_) => "chatDeleteUser",
        tl::enums::MessageAction::ChatJoinedByLink(_) => "chatJoinedByLink",
        tl::enums::MessageAction::ChannelCreate(_) => "channelCreate",
        tl::enums::MessageAction::ChatMigrateTo(_) => "chatMigrateTo",
        tl::enums::MessageAction::ChannelMigrateFrom(_) => "channelMigrateFrom",
        tl::enums::MessageAction::PinMessage => "pinMessage",
        tl::enums::MessageAction::HistoryClear => "historyClear",
        tl::enums::MessageAction::GameScore(_) => "gameScore",
        tl::enums::MessageAction::PaymentSentMe(_) => "paymentSentMe",
        tl::enums::MessageAction::PaymentSent(_) => "paymentSent",
        tl::enums::MessageAction::PhoneCall(_) => "phoneCall",
        tl::enums::MessageAction::ScreenshotTaken => "screenshotTaken",
        tl::enums::MessageAction::CustomAction(_) => "customAction",
        tl::enums::MessageAction::BotAllowed(_) => "botAllowed",
        tl::enums::MessageAction::SecureValuesSentMe(_) => "secureValuesSentMe",
        tl::enums::MessageAction::SecureValuesSent(_) => "secureValuesSent",
        tl::enums::MessageAction::ContactSignUp => "contactSignUp",
        tl::enums::MessageAction::GeoProximityReached(_) => "geoProximityReached",
        tl::enums::MessageAction::GroupCall(_) => "groupCall",
        tl::enums::MessageAction::InviteToGroupCall(_) => "inviteToGroupCall",
        tl::enums::MessageAction::SetMessagesTtl(_) => "setMessagesTtl",
        tl::enums::MessageAction::GroupCallScheduled(_) => "groupCallScheduled",
        tl::enums::MessageAction::SetChatTheme(_) => "setChatTheme",
        tl::enums::MessageAction::ChatJoinedByRequest => "chatJoinedByRequest",
        tl::enums::MessageAction::WebViewDataSentMe(_) => "webViewDataSentMe",
        tl::enums::MessageAction::WebViewDataSent(_) => "webViewDataSent",
        tl::enums::MessageAction::GiftPremium(_) => "giftPremium",
        tl::enums::MessageAction::TopicCreate(_) => "topicCreate",
        tl::enums::MessageAction::TopicEdit(_) => "topicEdit",
        tl::enums::MessageAction::SuggestProfilePhoto(_) => "suggestProfilePhoto",
        tl::enums::MessageAction::RequestedPeer(_) => "requestedPeer",
        tl::enums::MessageAction::SetChatWallPaper(_) => "setChatWallPaper",
        tl::enums::MessageAction::GiftCode(_) => "giftCode",
        tl::enums::MessageAction::GiveawayLaunch(_) => "giveawayLaunch",
        tl::enums::MessageAction::GiveawayResults(_) => "giveawayResults",
        tl::enums::MessageAction::BoostApply(_) => "boostApply",
        tl::enums::MessageAction::RequestedPeerSentMe(_) => "requestedPeerSentMe",
        tl::enums::MessageAction::PaymentRefunded(_) => "paymentRefunded",
        tl::enums::MessageAction::GiftStars(_) => "giftStars",
        tl::enums::MessageAction::PrizeStars(_) => "prizeStars",
        tl::enums::MessageAction::StarGift(_) => "starGift",
        tl::enums::MessageAction::StarGiftUnique(_) => "starGiftUnique",
        tl::enums::MessageAction::PaidMessagesRefunded(_) => "paidMessagesRefunded",
        tl::enums::MessageAction::PaidMessagesPrice(_) => "paidMessagesPrice",
        tl::enums::MessageAction::ConferenceCall(_) => "conferenceCall",
        tl::enums::MessageAction::TodoCompletions(_) => "todoCompletions",
        tl::enums::MessageAction::TodoAppendTasks(_) => "todoAppendTasks",
        tl::enums::MessageAction::SuggestedPostApproval(_) => "suggestedPostApproval",
        tl::enums::MessageAction::SuggestedPostSuccess(_) => "suggestedPostSuccess",
        tl::enums::MessageAction::SuggestedPostRefund(_) => "suggestedPostRefund",
        tl::enums::MessageAction::GiftTon(_) => "giftTon",
        tl::enums::MessageAction::SuggestBirthday(_) => "suggestBirthday",
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    /// Asserts that [dto] encodes to exactly [expected].
    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    #[test]
    fn a_service_action_encodes_its_message_sender_and_kind() {
        assert_json(
            &MessageActionDto {
                message_id: 31,
                sender_id: Some(7),
                kind: "pinMessage",
            },
            json!({ "messageId": 31, "senderId": 7, "kind": "pinMessage" }),
        );
    }

    #[test]
    fn a_service_action_without_a_sender_reports_a_null_one() {
        assert_json(
            &MessageActionDto {
                message_id: 4,
                sender_id: None,
                kind: "historyClear",
            },
            json!({ "messageId": 4, "senderId": null, "kind": "historyClear" }),
        );
    }

    #[test]
    fn a_layer_variant_is_named_after_itself() {
        assert_eq!(
            action_kind(&tl::enums::MessageAction::PinMessage),
            "pinMessage"
        );
        assert_eq!(action_kind(&tl::enums::MessageAction::Empty), "empty");
        assert_eq!(
            action_kind(&tl::enums::MessageAction::ContactSignUp),
            "contactSignUp"
        );
    }

    #[test]
    fn a_variant_carrying_a_payload_is_named_too() {
        let action =
            tl::enums::MessageAction::ChatEditTitle(tl::types::MessageActionChatEditTitle {
                title: "Renamed".to_owned(),
            });
        assert_eq!(action_kind(&action), "chatEditTitle");
    }
}
