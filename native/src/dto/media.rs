//! Message-media projection.
//!
//! grammers models an attachment as [`Media`], an enum with one variant per attachment type and no
//! shared accessor surface: only `Photo`, `Document` and `Sticker` have an identifier, only
//! `Document` has a name, and only `WebPage` has a URL. Projecting that as a tagged union would
//! mean ten shapes and a discriminator field that Kotlin has to unwrap twice, so the bridge sends
//! one flat object whose `kind` says which of the optional fields are populated. Every field is
//! always present in the JSON; the ones that do not apply to a kind are `null`.

use grammers_client::grammers_tl_types as tl;
use grammers_client::types::photo_sizes::{PhotoSize, VecExt};
use grammers_client::types::Media as ClientMedia;
use serde::Serialize;

/// The media attached to a message, flattened as described in the module docs.
///
/// [Self::kind] is the grammers `Media` variant in lowerCamelCase, or `unknown` for a variant this
/// bridge does not model yet. `unknown` still carries the object, so a media this build does not
/// understand stays distinguishable from a message that has no media at all.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct MediaDto {
    pub(crate) kind: &'static str,

    // Photos, documents and stickers.
    pub(crate) id: Option<i64>,
    pub(crate) size: Option<i64>,
    pub(crate) width: Option<i32>,
    pub(crate) height: Option<i32>,
    pub(crate) spoiler: Option<bool>,
    pub(crate) ttl_seconds: Option<i32>,
    pub(crate) name: Option<String>,
    pub(crate) mime_type: Option<String>,
    pub(crate) creation_date: Option<i64>,
    pub(crate) duration: Option<f64>,
    pub(crate) resolution_width: Option<i32>,
    pub(crate) resolution_height: Option<i32>,
    pub(crate) audio_title: Option<String>,
    pub(crate) performer: Option<String>,
    /// The sticker emoji, or the dice emoticon.
    pub(crate) emoji: Option<String>,
    pub(crate) is_animated: Option<bool>,

    // Contacts.
    pub(crate) phone_number: Option<String>,
    pub(crate) first_name: Option<String>,
    pub(crate) last_name: Option<String>,
    pub(crate) vcard: Option<String>,

    // Polls.
    pub(crate) question: Option<String>,
    pub(crate) is_quiz: Option<bool>,
    pub(crate) closed: Option<bool>,
    pub(crate) total_voters: Option<i32>,

    // Locations, venues and live locations.
    pub(crate) latitude: Option<f64>,
    pub(crate) longitude: Option<f64>,
    pub(crate) accuracy_radius: Option<i32>,
    pub(crate) title: Option<String>,
    pub(crate) address: Option<String>,
    pub(crate) provider: Option<String>,
    pub(crate) venue_id: Option<String>,
    pub(crate) venue_type: Option<String>,
    pub(crate) heading: Option<i32>,
    pub(crate) period: Option<i32>,
    pub(crate) proximity_notification_radius: Option<i32>,

    // Dice.
    pub(crate) value: Option<i32>,

    // Web page previews.
    pub(crate) url: Option<String>,
    pub(crate) display_url: Option<String>,
    pub(crate) site_name: Option<String>,
    pub(crate) description: Option<String>,
    /// The web page's `type` flag, for example `video` or `article`.
    pub(crate) page_type: Option<String>,
    pub(crate) author: Option<String>,
}

impl MediaDto {
    /// A projection with nothing but [kind] set. Every variant arm starts here and fills in the
    /// accessors its own kind actually has, which keeps the field list in one place.
    pub(crate) fn empty(kind: &'static str) -> Self {
        Self {
            kind,
            id: None,
            size: None,
            width: None,
            height: None,
            spoiler: None,
            ttl_seconds: None,
            name: None,
            mime_type: None,
            creation_date: None,
            duration: None,
            resolution_width: None,
            resolution_height: None,
            audio_title: None,
            performer: None,
            emoji: None,
            is_animated: None,
            phone_number: None,
            first_name: None,
            last_name: None,
            vcard: None,
            question: None,
            is_quiz: None,
            closed: None,
            total_voters: None,
            latitude: None,
            longitude: None,
            accuracy_radius: None,
            title: None,
            address: None,
            provider: None,
            venue_id: None,
            venue_type: None,
            heading: None,
            period: None,
            proximity_notification_radius: None,
            value: None,
            url: None,
            display_url: None,
            site_name: None,
            description: None,
            page_type: None,
            author: None,
        }
    }
}

/// Projects the media of a message.
///
/// `Message::media()` already collapsed the attachment types grammers does not model (games,
/// invoices, stories, giveaways, paid media, to-dos) into `None`, so a media this function is
/// handed is either one of the ten kinds below or a variant added after this bridge was written.
pub(crate) fn media_dto(media: &ClientMedia) -> MediaDto {
    match media {
        ClientMedia::Photo(photo) => {
            let mut dto = MediaDto::empty("photo");
            // `Photo::id` and `Photo::size` unwrap the inner photo, which the layer may leave
            // unset behind the `photo` flag; `thumbs` copes with the same absence.
            if photo.raw.photo.is_some() {
                dto.id = Some(photo.id());
                dto.size = Some(photo.size());
            }
            let (width, height) = dimensions(photo.thumbs().largest());
            dto.width = width;
            dto.height = height;
            dto.spoiler = Some(photo.is_spoiler());
            dto.ttl_seconds = photo.ttl_seconds();
            dto
        }
        ClientMedia::Document(document) => document_dto(document),
        ClientMedia::Sticker(sticker) => {
            let mut dto = document_dto(&sticker.document);
            dto.kind = "sticker";
            dto.emoji = Some(sticker.emoji().to_owned());
            dto.is_animated = Some(sticker.is_animated());
            dto
        }
        ClientMedia::Contact(contact) => {
            let mut dto = MediaDto::empty("contact");
            dto.phone_number = Some(contact.phone_number().to_owned());
            dto.first_name = Some(contact.first_name().to_owned());
            dto.last_name = Some(contact.last_name().to_owned());
            dto.vcard = Some(contact.vcard().to_owned());
            dto
        }
        ClientMedia::Poll(poll) => {
            let mut dto = MediaDto::empty("poll");
            dto.question = Some(match poll.question() {
                tl::enums::TextWithEntities::Entities(question) => question.text.clone(),
            });
            dto.is_quiz = Some(poll.is_quiz());
            dto.closed = Some(poll.closed());
            dto.total_voters = poll.total_voters();
            dto
        }
        ClientMedia::Geo(geo) => {
            let mut dto = MediaDto::empty("geo");
            fill_geo(&mut dto, geo);
            dto
        }
        ClientMedia::Dice(dice) => {
            let mut dto = MediaDto::empty("dice");
            dto.emoji = Some(dice.emoji().to_owned());
            dto.value = Some(dice.value());
            dto
        }
        ClientMedia::Venue(venue) => {
            let mut dto = MediaDto::empty("venue");
            if let Some(geo) = &venue.geo {
                fill_geo(&mut dto, geo);
            }
            dto.title = Some(venue.title().to_owned());
            dto.address = Some(venue.address().to_owned());
            dto.provider = Some(venue.provider().to_owned());
            dto.venue_id = Some(venue.venue_id().to_owned());
            dto.venue_type = Some(venue.venue_type().to_owned());
            dto
        }
        ClientMedia::GeoLive(live) => {
            let mut dto = MediaDto::empty("geoLive");
            if let Some(geo) = &live.geo {
                fill_geo(&mut dto, geo);
            }
            dto.heading = live.heading();
            dto.period = Some(live.period());
            dto.proximity_notification_radius = live.proximity_notification_radius();
            dto
        }
        ClientMedia::WebPage(web_page) => {
            let mut dto = MediaDto::empty("webPage");
            match &web_page.raw.webpage {
                tl::enums::WebPage::Empty(page) => {
                    dto.id = Some(page.id);
                    dto.url = page.url.clone();
                }
                tl::enums::WebPage::Pending(page) => {
                    dto.id = Some(page.id);
                    dto.url = page.url.clone();
                }
                tl::enums::WebPage::Page(page) => {
                    dto.id = Some(page.id);
                    dto.url = Some(page.url.clone());
                    dto.display_url = Some(page.display_url.clone());
                    dto.site_name = page.site_name.clone();
                    dto.title = page.title.clone();
                    dto.description = page.description.clone();
                    dto.page_type = page.r#type.clone();
                    dto.author = page.author.clone();
                }
                tl::enums::WebPage::NotModified(_) => {}
            }
            dto
        }
        // `Media` is `#[non_exhaustive]`: a variant added to grammers after this bridge was
        // written must still project, just without the fields it would have filled in.
        _ => MediaDto::empty("unknown"),
    }
}

/// The fields shared by a document and the document behind a sticker.
fn document_dto(document: &grammers_client::types::media::Document) -> MediaDto {
    let mut dto = MediaDto::empty("document");
    // `Document::id` and `Document::name` unwrap the inner document, which the layer may leave
    // unset behind the `document` flag; the other accessors already cope with its absence.
    if document.raw.document.is_some() {
        dto.id = Some(document.id());
        dto.name = non_empty(document.name());
    }
    let (width, height) = dimensions(document.thumbs().largest());
    dto.width = width;
    dto.height = height;
    dto.spoiler = Some(document.is_spoiler());
    dto.mime_type = document.mime_type().map(ToOwned::to_owned);
    dto.creation_date = document.creation_date().map(|date| date.timestamp_millis());
    dto.size = Some(document.size());
    dto.duration = document.duration();
    if let Some((width, height)) = document.resolution() {
        dto.resolution_width = Some(width);
        dto.resolution_height = Some(height);
    }
    dto.audio_title = document.audio_title();
    dto.performer = document.performer();
    dto.is_animated = Some(document.is_animated());
    dto
}

/// Copies a grammers point of interest onto the shared location fields.
fn fill_geo(dto: &mut MediaDto, geo: &grammers_client::types::media::Geo) {
    // grammers spells the latitude accessor `latitue`; the field is spelled correctly here.
    dto.latitude = Some(geo.latitue());
    dto.longitude = Some(geo.longitude());
    dto.accuracy_radius = geo.accuracy_radius();
}

/// The pixel size of a thumbnail. grammers only keeps dimensions on the `Size`, `Cached` and
/// `Progressive` variants; the others describe bytes without a resolution.
fn dimensions(thumb: Option<&PhotoSize>) -> (Option<i32>, Option<i32>) {
    match thumb {
        Some(PhotoSize::Size(size)) => (Some(size.width), Some(size.height)),
        Some(PhotoSize::Cached(size)) => (Some(size.width), Some(size.height)),
        Some(PhotoSize::Progressive(size)) => (Some(size.width), Some(size.height)),
        _ => (None, None),
    }
}

/// Turns grammers' empty-string-for-absent convention into `None`, as the other names do.
fn non_empty(value: &str) -> Option<String> {
    (!value.is_empty()).then(|| value.to_owned())
}
