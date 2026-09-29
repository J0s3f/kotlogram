//! Typed media: sending a photo, document or file with a caption, a parse mode, a MIME type, a
//! time-to-live and scheduling, and copying the media of an existing message.
//!
//! grammers attaches one attachment to an outgoing message through [`InputMessage`]: `photo`,
//! `document` and `file` cover the three ways an uploaded file is sent, `photo_url` and
//! `document_url` cover a file Telegram fetches itself, and `copy_media` reuses the media of a
//! message without a re-upload. The caption is the message text, parsed into formatting entities
//! by `html`, `markdown` or `text`; `media_ttl`, `mime_type`, `invert_media`, `silent`,
//! `reply_to`, `schedule_date` and `schedule_once_online` carry the remaining options.
//!
//! grammers' upload builders hard-code `spoiler: false` and expose no setter for it, so a spoiler
//! this bridge is asked to send is built from the raw `InputMediaUploaded*`/`InputMedia*External`
//! constructors instead, using the public `Uploaded::raw` handle the upload returns.

use std::path::PathBuf;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use grammers_client::grammers_tl_types as tl;
use grammers_client::types::media::Uploaded;
use grammers_client::InputMessage;
use serde::Deserialize;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::message_dto;
use crate::error::{error, invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendMediaPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    /// The local file to upload and attach.
    path: String,
    /// `photo`, `document` (the default) or `file`, which is grammers' `force_file` document.
    kind: Option<String>,
    caption: Option<String>,
    /// `html`, `markdown` or `none` (the default): how the caption is parsed into entities.
    parse_mode: Option<String>,
    spoiler: Option<bool>,
    mime_type: Option<String>,
    ttl_seconds: Option<i32>,
    invert_media: Option<bool>,
    silent: Option<bool>,
    reply_to_message_id: Option<i32>,
    /// Epoch milliseconds at which to schedule the message.
    schedule_date: Option<i64>,
    schedule_once_online: Option<bool>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendMediaUrlPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    /// The URL Telegram downloads the media from.
    url: String,
    /// `photo` or `document` (the default).
    kind: Option<String>,
    caption: Option<String>,
    parse_mode: Option<String>,
    spoiler: Option<bool>,
    mime_type: Option<String>,
    ttl_seconds: Option<i32>,
    invert_media: Option<bool>,
    silent: Option<bool>,
    reply_to_message_id: Option<i32>,
    schedule_date: Option<i64>,
    schedule_once_online: Option<bool>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct CopyMediaPayload {
    destination: PeerTarget,
    source: PeerTarget,
    message_id: i32,
    caption: Option<String>,
    parse_mode: Option<String>,
    silent: Option<bool>,
    reply_to_message_id: Option<i32>,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &["sendMedia", "sendMediaUrl", "copyMedia"];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "sendMedia" => send_media,
        "sendMediaUrl" => send_media_url,
        "copyMedia" => copy_media,
        _ => return None,
    })
}

fn send_media(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SendMediaPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let kind = media_kind(data.kind.as_deref())?;
    let path = PathBuf::from(&data.path);
    let uploaded = native
        .runtime
        .block_on(native.client.upload_file(&path))
        .map_err(error)?;

    let mut message = message_options(
        data.caption,
        data.parse_mode.as_deref(),
        data.silent,
        data.reply_to_message_id,
        data.schedule_date,
        data.schedule_once_online,
        data.invert_media,
    )?;
    if let Some(ttl_seconds) = data.ttl_seconds {
        message = message.media_ttl(ttl_seconds);
    }
    if let Some(mime_type) = &data.mime_type {
        message = message.mime_type(mime_type);
    }
    // grammers' photo/document/file builders force `spoiler: false`; the raw constructors carry it.
    let message = if data.spoiler.unwrap_or(false) {
        let file_name = file_name(&path);
        message.media(raw_uploaded_media(
            kind,
            uploaded,
            &file_name,
            true,
            data.mime_type.as_deref(),
            data.ttl_seconds,
        ))
    } else {
        match kind {
            MediaKind::Photo => message.photo(uploaded),
            MediaKind::File => message.file(uploaded),
            MediaKind::Document => message.document(uploaded),
        }
    };

    let message = native
        .runtime
        .block_on(native.client.send_message(peer, message))
        .map_err(invocation_error)?;
    json_string(message_dto(&message))
}

fn send_media_url(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SendMediaUrlPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let kind = media_kind(data.kind.as_deref())?;

    let mut message = message_options(
        data.caption,
        data.parse_mode.as_deref(),
        data.silent,
        data.reply_to_message_id,
        data.schedule_date,
        data.schedule_once_online,
        data.invert_media,
    )?;
    if let Some(ttl_seconds) = data.ttl_seconds {
        message = message.media_ttl(ttl_seconds);
    }
    if let Some(mime_type) = &data.mime_type {
        message = message.mime_type(mime_type);
    }
    let message = if data.spoiler.unwrap_or(false) {
        message.media(raw_external_media(kind, &data.url, true, data.ttl_seconds))
    } else if kind == MediaKind::Photo {
        message.photo_url(data.url)
    } else {
        message.document_url(data.url)
    };

    let message = native
        .runtime
        .block_on(native.client.send_message(peer, message))
        .map_err(invocation_error)?;
    json_string(message_dto(&message))
}

fn copy_media(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: CopyMediaPayload = parse_payload(payload)?;
    let destination = native
        .runtime
        .block_on(resolve_peer(native, &data.destination))?;
    let source = native
        .runtime
        .block_on(resolve_peer(native, &data.source))?;
    let messages = native
        .runtime
        .block_on(native.client.get_messages_by_id(source, &[data.message_id]))
        .map_err(invocation_error)?;
    let message = messages.into_iter().next().flatten().ok_or_else(|| {
        format!(
            "message {} was not found in the source chat",
            data.message_id
        )
    })?;
    let media = message
        .media()
        .ok_or_else(|| "the source message carries no media to copy".to_owned())?;
    // A web page preview, and any variant a later layer adds, has no input counterpart.
    if media.to_raw_input_media().is_none() {
        return Err("the source message's media cannot be copied".to_owned());
    }

    let message = message_options(
        data.caption,
        data.parse_mode.as_deref(),
        data.silent,
        data.reply_to_message_id,
        None,
        None,
        None,
    )?;
    let message = native
        .runtime
        .block_on(
            native
                .client
                .send_message(destination, message.copy_media(&media)),
        )
        .map_err(invocation_error)?;
    json_string(message_dto(&message))
}

/// How a file is attached to an outgoing message.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum MediaKind {
    /// Telegram compresses the image and may convert it to JPEG; grammers' `photo`.
    Photo,
    /// Telegram inspects the file and may treat it as video or audio; grammers' `document`.
    Document,
    /// The file is sent verbatim; grammers' `file`, which is `force_file`.
    File,
}

/// The markup a caption is parsed with, or the plain text grammers' `text` sends.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum CaptionMode {
    Plain,
    Html,
    Markdown,
}

fn media_kind(kind: Option<&str>) -> Result<MediaKind, String> {
    match kind.map(str::to_ascii_lowercase).as_deref() {
        None | Some("") | Some("document") => Ok(MediaKind::Document),
        Some("photo") => Ok(MediaKind::Photo),
        Some("file") => Ok(MediaKind::File),
        Some(other) => Err(format!("unsupported media kind: {other}")),
    }
}

fn caption_mode(parse_mode: Option<&str>) -> Result<CaptionMode, String> {
    match parse_mode.map(str::to_ascii_lowercase).as_deref() {
        None | Some("") | Some("none") => Ok(CaptionMode::Plain),
        Some("html") => Ok(CaptionMode::Html),
        Some("markdown") => Ok(CaptionMode::Markdown),
        Some(other) => Err(format!("unsupported parse mode: {other}")),
    }
}

/// Builds the options every send shares: the parsed caption and the message flags.
fn message_options(
    caption: Option<String>,
    parse_mode: Option<&str>,
    silent: Option<bool>,
    reply_to_message_id: Option<i32>,
    schedule_date: Option<i64>,
    schedule_once_online: Option<bool>,
    invert_media: Option<bool>,
) -> Result<InputMessage, String> {
    let caption = caption.unwrap_or_default();
    let mut message = match caption_mode(parse_mode)? {
        CaptionMode::Plain => InputMessage::new().text(caption),
        CaptionMode::Html => InputMessage::new().html(caption),
        CaptionMode::Markdown => InputMessage::new().markdown(caption),
    };
    if invert_media.unwrap_or(false) {
        message = message.invert_media(true);
    }
    message = message.silent(silent.unwrap_or(false));
    message = message.reply_to(reply_to_message_id);
    if schedule_once_online.unwrap_or(false) {
        message = message.schedule_once_online();
    } else if let Some(millis) = schedule_date {
        message = message.schedule_date(Some(schedule_time(millis)?));
    }
    Ok(message)
}

/// Converts the epoch milliseconds the bridge carries into the [`SystemTime`] grammers schedules
/// with.
fn schedule_time(millis: i64) -> Result<SystemTime, String> {
    let millis = u64::try_from(millis)
        .map_err(|_| format!("the schedule date must not be before the epoch: {millis}"))?;
    Ok(UNIX_EPOCH + Duration::from_millis(millis))
}

/// The name a document carries; grammers takes it from the uploaded file, which the path names.
fn file_name(path: &PathBuf) -> String {
    path.file_name()
        .map(|name| name.to_string_lossy().into_owned())
        .unwrap_or_default()
}

/// Builds the raw uploaded media the `spoiler` flag requires, matching what grammers' own
/// `photo`/`document`/`file` builders produce otherwise.
fn raw_uploaded_media(
    kind: MediaKind,
    uploaded: Uploaded,
    file_name: &str,
    spoiler: bool,
    mime_type: Option<&str>,
    ttl_seconds: Option<i32>,
) -> tl::enums::InputMedia {
    match kind {
        MediaKind::Photo => tl::types::InputMediaUploadedPhoto {
            spoiler,
            file: uploaded.raw,
            stickers: None,
            ttl_seconds,
        }
        .into(),
        MediaKind::Document | MediaKind::File => tl::types::InputMediaUploadedDocument {
            nosound_video: false,
            force_file: kind == MediaKind::File,
            spoiler,
            file: uploaded.raw,
            thumb: None,
            mime_type: mime_type
                .map(ToOwned::to_owned)
                .unwrap_or_else(|| inferred_mime(file_name)),
            attributes: vec![tl::types::DocumentAttributeFilename {
                file_name: file_name.to_owned(),
            }
            .into()],
            stickers: None,
            ttl_seconds,
            video_cover: None,
            video_timestamp: None,
        }
        .into(),
    }
}

/// Builds the raw external media the `spoiler` flag requires, matching grammers' own
/// `photo_url`/`document_url` builders otherwise.
fn raw_external_media(
    kind: MediaKind,
    url: &str,
    spoiler: bool,
    ttl_seconds: Option<i32>,
) -> tl::enums::InputMedia {
    match kind {
        MediaKind::Photo => tl::types::InputMediaPhotoExternal {
            spoiler,
            url: url.to_owned(),
            ttl_seconds,
        }
        .into(),
        MediaKind::Document | MediaKind::File => tl::types::InputMediaDocumentExternal {
            spoiler,
            url: url.to_owned(),
            ttl_seconds,
            video_cover: None,
            video_timestamp: None,
        }
        .into(),
    }
}

/// The MIME type grammers would infer from the extension, for the raw path it cannot infer on.
fn inferred_mime(file_name: &str) -> String {
    mime_guess::from_path(file_name)
        .first()
        .map(|mime| mime.essence_str().to_owned())
        .unwrap_or_else(|| "application/octet-stream".to_owned())
}

#[cfg(test)]
mod tests {
    //! Tests for the request side: the kind and parse-mode vocabulary, the caption and schedule
    //! conversion, and the raw media the spoiler path hands grammers.
    //!
    //! The handlers themselves need a live Telegram session, so what is asserted here is everything
    //! between the payload and the grammers call. The JSON documents below describe the built
    //! `grammers-tl-types` value, which carries no serde derives.

    use std::path::PathBuf;
    use std::time::{Duration, UNIX_EPOCH};

    use grammers_client::grammers_tl_types as tl;
    use grammers_client::types::media::Uploaded;
    use serde_json::json;

    use crate::error::parse_payload;

    use super::{
        caption_mode, file_name, inferred_mime, media_kind, raw_external_media, raw_uploaded_media,
        schedule_time, CaptionMode, CopyMediaPayload, MediaKind, SendMediaPayload,
        SendMediaUrlPayload,
    };

    fn uploaded() -> Uploaded {
        Uploaded::from_raw(tl::enums::InputFile::File(tl::types::InputFile {
            id: 4242,
            parts: 1,
            name: "report.pdf".to_owned(),
            md5_checksum: String::new(),
        }))
    }

    fn file_json(file: &tl::enums::InputFile) -> serde_json::Value {
        match file {
            tl::enums::InputFile::File(file) => json!({
                "kind": "file",
                "id": file.id,
                "parts": file.parts,
                "name": file.name,
            }),
            tl::enums::InputFile::Big(file) => json!({
                "kind": "big",
                "id": file.id,
                "parts": file.parts,
                "name": file.name,
            }),
            other => json!({ "kind": format!("{other:?}") }),
        }
    }

    fn attributes_json(attributes: &[tl::enums::DocumentAttribute]) -> serde_json::Value {
        serde_json::Value::Array(
            attributes
                .iter()
                .map(|attribute| match attribute {
                    tl::enums::DocumentAttribute::Filename(file) => json!({
                        "kind": "filename",
                        "fileName": file.file_name,
                    }),
                    other => json!({ "kind": format!("{other:?}") }),
                })
                .collect(),
        )
    }

    /// The exact document the built input media stands for.
    fn media_json(media: &tl::enums::InputMedia) -> serde_json::Value {
        match media {
            tl::enums::InputMedia::UploadedPhoto(photo) => json!({
                "kind": "uploadedPhoto",
                "spoiler": photo.spoiler,
                "file": file_json(&photo.file),
                "ttlSeconds": photo.ttl_seconds,
            }),
            tl::enums::InputMedia::UploadedDocument(document) => json!({
                "kind": "uploadedDocument",
                "nosoundVideo": document.nosound_video,
                "forceFile": document.force_file,
                "spoiler": document.spoiler,
                "file": file_json(&document.file),
                "mimeType": document.mime_type,
                "attributes": attributes_json(&document.attributes),
                "ttlSeconds": document.ttl_seconds,
            }),
            tl::enums::InputMedia::PhotoExternal(photo) => json!({
                "kind": "photoExternal",
                "spoiler": photo.spoiler,
                "url": photo.url,
                "ttlSeconds": photo.ttl_seconds,
            }),
            tl::enums::InputMedia::DocumentExternal(document) => json!({
                "kind": "documentExternal",
                "spoiler": document.spoiler,
                "url": document.url,
                "ttlSeconds": document.ttl_seconds,
            }),
            other => json!({ "kind": format!("{other:?}") }),
        }
    }

    #[test]
    fn a_media_kind_names_exactly_the_three_grammers_builders() {
        assert_eq!(media_kind(None), Ok(MediaKind::Document));
        assert_eq!(media_kind(Some("")), Ok(MediaKind::Document));
        assert_eq!(media_kind(Some("document")), Ok(MediaKind::Document));
        assert_eq!(media_kind(Some("photo")), Ok(MediaKind::Photo));
        assert_eq!(media_kind(Some("file")), Ok(MediaKind::File));
        assert_eq!(
            media_kind(Some("sticker")),
            Err("unsupported media kind: sticker".to_owned())
        );
    }

    #[test]
    fn a_parse_mode_names_exactly_the_three_grammers_caption_builders() {
        assert_eq!(caption_mode(None), Ok(CaptionMode::Plain));
        assert_eq!(caption_mode(Some("none")), Ok(CaptionMode::Plain));
        assert_eq!(caption_mode(Some("html")), Ok(CaptionMode::Html));
        assert_eq!(caption_mode(Some("markdown")), Ok(CaptionMode::Markdown));
        assert_eq!(
            caption_mode(Some("json")),
            Err("unsupported parse mode: json".to_owned())
        );
    }

    #[test]
    fn a_spoiler_document_is_built_with_the_file_name_and_inferred_mime() {
        let media = raw_uploaded_media(
            MediaKind::Document,
            uploaded(),
            "report.pdf",
            true,
            None,
            Some(60),
        );

        assert_eq!(
            media_json(&media),
            json!({
                "kind": "uploadedDocument",
                "nosoundVideo": false,
                "forceFile": false,
                "spoiler": true,
                "file": { "kind": "file", "id": 4242, "parts": 1, "name": "report.pdf" },
                "mimeType": "application/pdf",
                "attributes": [{ "kind": "filename", "fileName": "report.pdf" }],
                "ttlSeconds": 60,
            })
        );
    }

    #[test]
    fn a_file_kind_forces_the_generic_document() {
        let media = raw_uploaded_media(
            MediaKind::File,
            uploaded(),
            "archive.bin",
            true,
            Some("application/x-custom"),
            None,
        );

        assert_eq!(
            media_json(&media),
            json!({
                "kind": "uploadedDocument",
                "nosoundVideo": false,
                "forceFile": true,
                "spoiler": true,
                "file": { "kind": "file", "id": 4242, "parts": 1, "name": "report.pdf" },
                "mimeType": "application/x-custom",
                "attributes": [{ "kind": "filename", "fileName": "archive.bin" }],
                "ttlSeconds": null,
            })
        );
    }

    #[test]
    fn a_spoiler_photo_carries_no_name_or_mime() {
        let media = raw_uploaded_media(MediaKind::Photo, uploaded(), "x.png", true, None, Some(7));

        assert_eq!(
            media_json(&media),
            json!({
                "kind": "uploadedPhoto",
                "spoiler": true,
                "file": { "kind": "file", "id": 4242, "parts": 1, "name": "report.pdf" },
                "ttlSeconds": 7,
            })
        );
    }

    #[test]
    fn an_external_media_keeps_its_url_spoiler_and_ttl() {
        let photo =
            raw_external_media(MediaKind::Photo, "https://example.org/a.jpg", true, Some(9));
        assert_eq!(
            media_json(&photo),
            json!({
                "kind": "photoExternal",
                "spoiler": true,
                "url": "https://example.org/a.jpg",
                "ttlSeconds": 9,
            })
        );

        let document = raw_external_media(
            MediaKind::Document,
            "https://example.org/a.pdf",
            false,
            None,
        );
        assert_eq!(
            media_json(&document),
            json!({
                "kind": "documentExternal",
                "spoiler": false,
                "url": "https://example.org/a.pdf",
                "ttlSeconds": null,
            })
        );
    }

    #[test]
    fn the_file_name_comes_from_the_path() {
        assert_eq!(file_name(&PathBuf::from("/tmp/report.pdf")), "report.pdf");
        assert_eq!(inferred_mime("report.pdf"), "application/pdf");
        assert_eq!(
            inferred_mime("unknown.unknown-extension"),
            "application/octet-stream"
        );
    }

    #[test]
    fn a_schedule_date_is_epoch_milliseconds() {
        assert_eq!(
            schedule_time(1_700_000_000_000).expect("after the epoch"),
            UNIX_EPOCH + Duration::from_millis(1_700_000_000_000)
        );
        assert!(schedule_time(-1).is_err());
    }

    #[test]
    fn the_send_media_payload_decodes_every_option() {
        let data: SendMediaPayload = parse_payload(
            &json!({
                "peerHandle": 12,
                "path": "/tmp/report.pdf",
                "kind": "file",
                "caption": "<b>hi</b>",
                "parseMode": "html",
                "spoiler": true,
                "mimeType": "application/pdf",
                "ttlSeconds": 30,
                "invertMedia": true,
                "silent": true,
                "replyToMessageId": 31,
                "scheduleDate": 1_700_000_000_000i64,
                "scheduleOnceOnline": true,
            })
            .to_string(),
        )
        .expect("the payload decodes");

        assert_eq!(data.peer.peer_handle, Some(12));
        assert_eq!(data.path, "/tmp/report.pdf");
        assert_eq!(data.kind.as_deref(), Some("file"));
        assert_eq!(data.caption.as_deref(), Some("<b>hi</b>"));
        assert_eq!(data.parse_mode.as_deref(), Some("html"));
        assert_eq!(data.spoiler, Some(true));
        assert_eq!(data.mime_type.as_deref(), Some("application/pdf"));
        assert_eq!(data.ttl_seconds, Some(30));
        assert_eq!(data.invert_media, Some(true));
        assert_eq!(data.silent, Some(true));
        assert_eq!(data.reply_to_message_id, Some(31));
        assert_eq!(data.schedule_date, Some(1_700_000_000_000));
        assert_eq!(data.schedule_once_online, Some(true));
    }

    #[test]
    fn a_minimal_send_payload_leaves_every_default_unset() {
        let data: SendMediaPayload =
            parse_payload(&json!({ "username": "channel", "path": "/tmp/a.bin" }).to_string())
                .expect("the payload decodes");

        assert_eq!(data.peer.username.as_deref(), Some("channel"));
        assert_eq!(data.kind, None);
        assert_eq!(data.caption, None);
        assert_eq!(data.parse_mode, None);
        assert_eq!(data.spoiler, None);
        assert_eq!(data.ttl_seconds, None);
    }

    #[test]
    fn the_url_and_copy_payloads_decode_their_own_fields() {
        let url: SendMediaUrlPayload = parse_payload(
            &json!({ "peerHandle": 1, "url": "https://example.org/a.jpg", "kind": "photo" })
                .to_string(),
        )
        .expect("the payload decodes");
        assert_eq!(url.url, "https://example.org/a.jpg");
        assert_eq!(url.kind.as_deref(), Some("photo"));
        assert_eq!(url.ttl_seconds, None);

        let copy: CopyMediaPayload = parse_payload(
            &json!({
                "destination": { "peerHandle": 1 },
                "source": { "username": "channel" },
                "messageId": 31,
                "caption": "copied",
            })
            .to_string(),
        )
        .expect("the payload decodes");
        assert_eq!(copy.destination.peer_handle, Some(1));
        assert_eq!(copy.source.username.as_deref(), Some("channel"));
        assert_eq!(copy.message_id, 31);
        assert_eq!(copy.caption.as_deref(), Some("copied"));
        assert_eq!(copy.parse_mode, None);
    }
}
