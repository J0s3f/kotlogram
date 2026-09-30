//! Sticker-set and sticker-pack projections.
//!
//! grammers has no typed sticker surface, so [`super::super::ops::stickers`] drives the layer's
//! `messages.*` functions directly and the results are projected here. The set summary keeps the
//! flag booleans and the date the layer reports (converted to epoch milliseconds, like the other
//! projections), and the thumbnail travels as the identifiers the summary carries rather than as
//! bytes. The packs and the documents behind a set are projected as the document ids only, which
//! is all a later send needs to reference them.

use grammers_client::tl;
use serde::Serialize;

/// One `stickerPack`: the emoticon and the ids of the documents it groups.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct StickerPackDto {
    pub(crate) emoticon: String,
    pub(crate) documents: Vec<i64>,
}

/// The `stickerSet` summary.
///
/// The pinned layer's `stickerSet` carries the `archived`, `official`, `masks`, `emojis`,
/// `textColor`, `channelEmojiStatus` and `creator` flags; the older `animated`/`videos` pair is
/// not part of this layer. The thumbnail is projected as the identifiers the layer reports
/// (`thumbDocumentId`, `thumbDcId`, `thumbVersion`), never as bytes.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct StickerSetDto {
    pub(crate) archived: bool,
    pub(crate) official: bool,
    pub(crate) masks: bool,
    pub(crate) emojis: bool,
    pub(crate) text_color: bool,
    pub(crate) channel_emoji_status: bool,
    pub(crate) creator: bool,
    /// Epoch milliseconds, from the layer's whole-second install date.
    pub(crate) installed_date: Option<i64>,
    pub(crate) id: i64,
    pub(crate) access_hash: i64,
    pub(crate) title: String,
    pub(crate) short_name: String,
    /// The layer's `thumbDocumentId`, the thumbnail's only identifier on the summary.
    pub(crate) thumb_document_id: Option<i64>,
    pub(crate) thumb_dc_id: Option<i32>,
    pub(crate) thumb_version: Option<i32>,
    pub(crate) count: i32,
    pub(crate) hash: i32,
    /// The packs the answer carried; empty for the list operations.
    pub(crate) packs: Vec<StickerPackDto>,
    /// The ids of the documents (stickers) the answer carried; empty for the list operations.
    pub(crate) documents: Vec<i64>,
}

/// The `messagesGetStickerSet` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct StickerSetResultDto {
    /// True for the layer's `messagesStickerSetNotModified`, which carries no set at all.
    pub(crate) not_modified: bool,
    pub(crate) set: Option<StickerSetDto>,
}

/// The `messagesGetAllStickers` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct AllStickersDto {
    pub(crate) not_modified: bool,
    pub(crate) hash: i64,
    pub(crate) sets: Vec<StickerSetDto>,
}

/// The `messagesGetRecentStickers` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RecentStickersDto {
    pub(crate) not_modified: bool,
    pub(crate) hash: i64,
    pub(crate) packs: Vec<StickerPackDto>,
    /// The ids of the recent stickers, in the answer's order.
    pub(crate) stickers: Vec<i64>,
    /// Epoch milliseconds for each sticker, matching [`Self::stickers`] by index.
    pub(crate) dates: Vec<i64>,
}

/// The `messagesGetFavedStickers` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FavedStickersDto {
    pub(crate) not_modified: bool,
    pub(crate) hash: i64,
    pub(crate) packs: Vec<StickerPackDto>,
    pub(crate) stickers: Vec<i64>,
}

/// Projects one `stickerPack`.
pub(crate) fn sticker_pack_dto(pack: &tl::enums::StickerPack) -> StickerPackDto {
    let tl::enums::StickerPack::Pack(pack) = pack;
    StickerPackDto {
        emoticon: pack.emoticon.clone(),
        documents: pack.documents.clone(),
    }
}

/// Projects a whole answer's pack vector.
pub(crate) fn sticker_packs_dto(packs: &[tl::enums::StickerPack]) -> Vec<StickerPackDto> {
    packs.iter().map(sticker_pack_dto).collect()
}

/// Projects one `stickerSet` summary, leaving the packs and documents empty.
pub(crate) fn sticker_set_dto(set: &tl::enums::StickerSet) -> StickerSetDto {
    let tl::enums::StickerSet::Set(set) = set;
    StickerSetDto {
        archived: set.archived,
        official: set.official,
        masks: set.masks,
        emojis: set.emojis,
        text_color: set.text_color,
        channel_emoji_status: set.channel_emoji_status,
        creator: set.creator,
        installed_date: set.installed_date.map(millis),
        id: set.id,
        access_hash: set.access_hash,
        title: set.title.clone(),
        short_name: set.short_name.clone(),
        thumb_document_id: set.thumb_document_id,
        thumb_dc_id: set.thumb_dc_id,
        thumb_version: set.thumb_version,
        count: set.count,
        hash: set.hash,
        packs: Vec::new(),
        documents: Vec::new(),
    }
}

/// Projects the full `messages.stickerSet`, attaching its packs and document ids.
pub(crate) fn full_sticker_set_dto(set: tl::types::messages::StickerSet) -> StickerSetResultDto {
    let mut summary = sticker_set_dto(&set.set);
    summary.packs = sticker_packs_dto(&set.packs);
    summary.documents = document_ids(&set.documents);
    StickerSetResultDto {
        not_modified: false,
        set: Some(summary),
    }
}

/// Projects `messages.allStickers`, answering the not-modified marker as an empty page.
pub(crate) fn all_stickers_dto(all: tl::enums::messages::AllStickers) -> AllStickersDto {
    match all {
        tl::enums::messages::AllStickers::NotModified => AllStickersDto {
            not_modified: true,
            hash: 0,
            sets: Vec::new(),
        },
        tl::enums::messages::AllStickers::Stickers(all) => AllStickersDto {
            not_modified: false,
            hash: all.hash,
            sets: all.sets.iter().map(sticker_set_dto).collect(),
        },
    }
}

/// Projects `messages.recentStickers`, answering the not-modified marker as an empty page.
pub(crate) fn recent_stickers_dto(
    recent: tl::enums::messages::RecentStickers,
) -> RecentStickersDto {
    match recent {
        tl::enums::messages::RecentStickers::NotModified => RecentStickersDto {
            not_modified: true,
            hash: 0,
            packs: Vec::new(),
            stickers: Vec::new(),
            dates: Vec::new(),
        },
        tl::enums::messages::RecentStickers::Stickers(recent) => RecentStickersDto {
            not_modified: false,
            hash: recent.hash,
            packs: sticker_packs_dto(&recent.packs),
            stickers: document_ids(&recent.stickers),
            dates: recent.dates.iter().copied().map(millis).collect(),
        },
    }
}

/// Projects `messages.favedStickers`, answering the not-modified marker as an empty page.
pub(crate) fn faved_stickers_dto(faved: tl::enums::messages::FavedStickers) -> FavedStickersDto {
    match faved {
        tl::enums::messages::FavedStickers::NotModified => FavedStickersDto {
            not_modified: true,
            hash: 0,
            packs: Vec::new(),
            stickers: Vec::new(),
        },
        tl::enums::messages::FavedStickers::Stickers(faved) => FavedStickersDto {
            not_modified: false,
            hash: faved.hash,
            packs: sticker_packs_dto(&faved.packs),
            stickers: document_ids(&faved.stickers),
        },
    }
}

/// The ids of a document vector, which is all a later send needs to reference a sticker.
pub(crate) fn document_ids(documents: &[tl::enums::Document]) -> Vec<i64> {
    documents.iter().map(tl::enums::Document::id).collect()
}

/// The layer reports dates in whole seconds; the wire format is epoch milliseconds.
fn millis(seconds: i32) -> i64 {
    i64::from(seconds) * 1_000
}

#[cfg(test)]
mod tests {
    //! Projection tests for the sticker DTOs, built from layer values by hand so every field is
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

    /// A `stickerSet` with flags, a thumbnail and a date.
    fn set() -> tl::types::StickerSet {
        tl::types::StickerSet {
            archived: false,
            official: true,
            masks: false,
            emojis: true,
            text_color: true,
            channel_emoji_status: false,
            creator: true,
            installed_date: Some(1_700_000_000),
            id: 1_234_567_890,
            access_hash: -9_876_543_210,
            title: "Some Pack".to_owned(),
            short_name: "somePack".to_owned(),
            thumbs: None,
            thumb_dc_id: Some(2),
            thumb_version: Some(7),
            thumb_document_id: Some(42),
            count: 12,
            hash: 99,
        }
    }

    fn pack() -> tl::enums::StickerPack {
        tl::enums::StickerPack::Pack(tl::types::StickerPack {
            emoticon: "🦄".to_owned(),
            documents: vec![42, 43],
        })
    }

    #[test]
    fn a_sticker_set_carries_its_flags_and_thumbnail_identifiers() {
        assert_json(
            &sticker_set_dto(&tl::enums::StickerSet::Set(set())),
            json!({
                "archived": false,
                "official": true,
                "masks": false,
                "emojis": true,
                "textColor": true,
                "channelEmojiStatus": false,
                "creator": true,
                "installedDate": 1_700_000_000_000i64,
                "id": 1_234_567_890i64,
                "accessHash": -9_876_543_210i64,
                "title": "Some Pack",
                "shortName": "somePack",
                "thumbDocumentId": 42,
                "thumbDcId": 2,
                "thumbVersion": 7,
                "count": 12,
                "hash": 99,
                "packs": [],
                "documents": [],
            }),
        );
    }

    #[test]
    fn a_set_without_a_thumbnail_or_date_leaves_them_null() {
        let mut set = set();
        set.installed_date = None;
        set.thumb_dc_id = None;
        set.thumb_version = None;
        set.thumb_document_id = None;
        let dto = sticker_set_dto(&tl::enums::StickerSet::Set(set));
        assert_eq!(dto.installed_date, None);
        assert_eq!(dto.thumb_document_id, None);
        assert_eq!(dto.thumb_dc_id, None);
        assert_eq!(dto.thumb_version, None);
    }

    #[test]
    fn a_pack_carries_its_emoticon_and_document_ids() {
        assert_json(
            &sticker_pack_dto(&pack()),
            json!({ "emoticon": "🦄", "documents": [42, 43] }),
        );
    }

    #[test]
    fn a_full_sticker_set_attaches_packs_and_documents() {
        let result = full_sticker_set_dto(tl::types::messages::StickerSet {
            set: tl::enums::StickerSet::Set(set()),
            packs: vec![pack()],
            keywords: vec![],
            documents: vec![tl::enums::Document::Empty(tl::types::DocumentEmpty {
                id: 42,
            })],
        });
        assert!(!result.not_modified);
        let summary = result.set.expect("the projection carries the set");
        assert_eq!(summary.packs.len(), 1);
        assert_eq!(summary.packs[0].documents, vec![42, 43]);
        assert_eq!(summary.documents, vec![42]);
    }

    #[test]
    fn a_not_modified_sticker_set_result_carries_no_set() {
        assert_json(
            &StickerSetResultDto {
                not_modified: true,
                set: None,
            },
            json!({ "notModified": true, "set": null }),
        );
    }

    #[test]
    fn all_stickers_carries_the_hash_and_the_sets() {
        let dto = all_stickers_dto(tl::enums::messages::AllStickers::Stickers(
            tl::types::messages::AllStickers {
                hash: 7,
                sets: vec![tl::enums::StickerSet::Set(set())],
            },
        ));
        assert!(!dto.not_modified);
        assert_eq!(dto.hash, 7);
        assert_eq!(dto.sets.len(), 1);
        assert_eq!(dto.sets[0].short_name, "somePack");
        assert!(dto.sets[0].packs.is_empty());
    }

    #[test]
    fn a_not_modified_all_stickers_is_an_empty_page() {
        assert_json(
            &all_stickers_dto(tl::enums::messages::AllStickers::NotModified),
            json!({ "notModified": true, "hash": 0, "sets": [] }),
        );
    }

    #[test]
    fn recent_stickers_carries_packs_ids_and_dates() {
        let dto = recent_stickers_dto(tl::enums::messages::RecentStickers::Stickers(
            tl::types::messages::RecentStickers {
                hash: 3,
                packs: vec![pack()],
                stickers: vec![tl::enums::Document::Empty(tl::types::DocumentEmpty {
                    id: 42,
                })],
                dates: vec![1_700_000_000],
            },
        ));
        assert!(!dto.not_modified);
        assert_eq!(dto.hash, 3);
        assert_eq!(dto.packs.len(), 1);
        assert_eq!(dto.stickers, vec![42]);
        assert_eq!(dto.dates, vec![1_700_000_000_000]);
    }

    #[test]
    fn a_not_modified_recent_stickers_is_an_empty_page() {
        assert_json(
            &recent_stickers_dto(tl::enums::messages::RecentStickers::NotModified),
            json!({
                "notModified": true,
                "hash": 0,
                "packs": [],
                "stickers": [],
                "dates": [],
            }),
        );
    }

    #[test]
    fn faved_stickers_carries_packs_and_ids() {
        let dto = faved_stickers_dto(tl::enums::messages::FavedStickers::Stickers(
            tl::types::messages::FavedStickers {
                hash: 5,
                packs: vec![],
                stickers: vec![tl::enums::Document::Empty(tl::types::DocumentEmpty {
                    id: 7,
                })],
            },
        ));
        assert!(!dto.not_modified);
        assert_eq!(dto.hash, 5);
        assert_eq!(dto.stickers, vec![7]);
    }

    #[test]
    fn a_not_modified_faved_stickers_is_an_empty_page() {
        assert_json(
            &faved_stickers_dto(tl::enums::messages::FavedStickers::NotModified),
            json!({ "notModified": true, "hash": 0, "packs": [], "stickers": [] }),
        );
    }

    #[test]
    fn the_layer_seconds_project_into_milliseconds() {
        assert_eq!(millis(1_700_000_000), 1_700_000_000_000);
        assert_eq!(millis(0), 0);
    }
}
