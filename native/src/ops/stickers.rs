//! Typed sticker operations built on grammers' TL layer.
//!
//! grammers exposes no sticker surface of its own, so this module follows the
//! [`super::contacts`] precedent: each operation builds one `tl::functions::messages::*` request,
//! invokes it through the client and projects the result through [`crate::dto::stickers`]. Every
//! operation here is get-only; installing, archiving and editing sets stay behind `invokeRaw`.

use grammers_client::tl;
use serde::Deserialize;

use super::Handler;
use crate::client::NativeClient;
use crate::dto::stickers::{
    all_stickers_dto, faved_stickers_dto, full_sticker_set_dto, recent_stickers_dto,
    StickerSetResultDto,
};
use crate::error::{invocation_error, json_string, parse_payload};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GetStickerSetPayload {
    /// The set's id, paired with `accessHash` when no short name is given.
    #[serde(default)]
    id: Option<i64>,
    #[serde(default)]
    access_hash: Option<i64>,
    /// The set's short name, which the layer resolves without an id.
    #[serde(default)]
    short_name: Option<String>,
    /// The layer's incremental hash; `0` asks for the full set.
    #[serde(default)]
    hash: i32,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GetStickersPayload {
    #[serde(default)]
    hash: i64,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GetRecentStickersPayload {
    /// True asks for the stickers attached to saved messages instead of the used ones.
    #[serde(default)]
    attached: bool,
    #[serde(default)]
    hash: i64,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "messagesGetStickerSet",
    "messagesGetAllStickers",
    "messagesGetRecentStickers",
    "messagesGetFavedStickers",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "messagesGetStickerSet" => get_sticker_set,
        "messagesGetAllStickers" => get_all_stickers,
        "messagesGetRecentStickers" => get_recent_stickers,
        "messagesGetFavedStickers" => get_faved_stickers,
        _ => return None,
    })
}

/// Reads one sticker set, answering the layer's `messagesStickerSetNotModified` as an empty
/// result.
fn get_sticker_set(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetStickerSetPayload = parse_payload(payload)?;
    let stickerset = input_sticker_set(&data)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::GetStickerSet {
                    stickerset,
                    hash: data.hash,
                }),
        )
        .map_err(invocation_error)?;
    json_string(match result {
        tl::enums::messages::StickerSet::NotModified => StickerSetResultDto {
            not_modified: true,
            set: None,
        },
        tl::enums::messages::StickerSet::Set(set) => full_sticker_set_dto(set),
    })
}

/// Lists the account's sticker sets, answering the layer's not-modified marker as an empty page.
fn get_all_stickers(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetStickersPayload = parse_payload(payload)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::GetAllStickers { hash: data.hash }),
        )
        .map_err(invocation_error)?;
    json_string(all_stickers_dto(result))
}

/// Lists the stickers the account recently used.
fn get_recent_stickers(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetRecentStickersPayload = parse_payload(payload)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::GetRecentStickers {
                    attached: data.attached,
                    hash: data.hash,
                }),
        )
        .map_err(invocation_error)?;
    json_string(recent_stickers_dto(result))
}

/// Lists the stickers the account has favourited.
fn get_faved_stickers(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetStickersPayload = parse_payload(payload)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::messages::GetFavedStickers { hash: data.hash }),
        )
        .map_err(invocation_error)?;
    json_string(faved_stickers_dto(result))
}

/// Maps the payload onto the layer's `InputStickerSet`, preferring a short name over the id pair.
fn input_sticker_set(data: &GetStickerSetPayload) -> Result<tl::enums::InputStickerSet, String> {
    if let Some(short_name) = data.short_name.as_deref().filter(|name| !name.is_empty()) {
        return Ok(tl::enums::InputStickerSet::ShortName(
            tl::types::InputStickerSetShortName {
                short_name: short_name.to_owned(),
            },
        ));
    }
    match (data.id, data.access_hash) {
        (Some(id), Some(access_hash)) => Ok(tl::enums::InputStickerSet::Id(
            tl::types::InputStickerSetId { id, access_hash },
        )),
        _ => Err("messagesGetStickerSet needs a shortName, or both id and accessHash".to_owned()),
    }
}

#[cfg(test)]
mod tests {
    //! Payload-decode tests and the input mapping.
    //!
    //! The handlers themselves need a live Telegram session, so what is pinned here is the wire
    //! shape Kotlin sends and the `InputStickerSet` the mapping produces.

    use super::*;

    fn decode<T: for<'de> Deserialize<'de>>(json: &str) -> T {
        serde_json::from_str(json).expect("the payload decodes")
    }

    #[test]
    fn a_sticker_set_payload_reads_the_short_name() {
        let data: GetStickerSetPayload = decode(r#"{"shortName": "somePack", "hash": 7}"#);
        assert_eq!(data.short_name.as_deref(), Some("somePack"));
        assert_eq!(data.hash, 7);
        assert_eq!(data.id, None);
        assert_eq!(data.access_hash, None);
    }

    #[test]
    fn a_sticker_set_payload_defaults_the_hash_to_zero() {
        let data: GetStickerSetPayload = decode("{}");
        assert_eq!(data.hash, 0);
        assert!(data.short_name.is_none());
    }

    #[test]
    fn a_short_name_maps_to_the_layer_input() {
        assert_eq!(
            input_sticker_set(&GetStickerSetPayload {
                id: Some(1),
                access_hash: Some(2),
                short_name: Some("somePack".to_owned()),
                hash: 0,
            })
            .expect("a short name"),
            tl::enums::InputStickerSet::ShortName(tl::types::InputStickerSetShortName {
                short_name: "somePack".to_owned(),
            })
        );
    }

    #[test]
    fn an_id_pair_maps_to_the_layer_input() {
        assert_eq!(
            input_sticker_set(&GetStickerSetPayload {
                id: Some(1_234_567_890),
                access_hash: Some(-9_876_543_210),
                short_name: None,
                hash: 0,
            })
            .expect("an id pair"),
            tl::enums::InputStickerSet::Id(tl::types::InputStickerSetId {
                id: 1_234_567_890,
                access_hash: -9_876_543_210,
            })
        );
    }

    #[test]
    fn an_empty_or_partial_sticker_set_payload_is_refused() {
        let error = input_sticker_set(&GetStickerSetPayload {
            id: None,
            access_hash: None,
            short_name: None,
            hash: 0,
        })
        .expect_err("no set");
        assert!(error.contains("messagesGetStickerSet needs a shortName"));

        let error = input_sticker_set(&GetStickerSetPayload {
            id: Some(1),
            access_hash: None,
            short_name: None,
            hash: 0,
        })
        .expect_err("an id alone is not enough");
        assert!(error.contains("both id and accessHash"));
    }

    #[test]
    fn a_recent_stickers_payload_reads_the_attached_flag_and_the_hash() {
        let data: GetRecentStickersPayload = decode(r#"{"attached": true, "hash": 9}"#);
        assert!(data.attached);
        assert_eq!(data.hash, 9);

        let data: GetRecentStickersPayload = decode("{}");
        assert!(!data.attached);
        assert_eq!(data.hash, 0);
    }

    #[test]
    fn a_get_stickers_payload_defaults_the_hash_to_zero() {
        assert_eq!(decode::<GetStickersPayload>("{}").hash, 0);
        assert_eq!(decode::<GetStickersPayload>(r#"{"hash": 42}"#).hash, 42);
    }

    #[test]
    fn the_sticker_operations_route_to_their_own_handler() {
        assert_eq!(
            route("messagesGetStickerSet"),
            Some(get_sticker_set as Handler)
        );
        assert_eq!(
            route("messagesGetFavedStickers"),
            Some(get_faved_stickers as Handler)
        );
    }

    #[test]
    fn a_foreign_operation_is_not_routed_by_this_module() {
        assert!(route("getMe").is_none());
        assert!(route("contactsGetContacts").is_none());
    }
}
