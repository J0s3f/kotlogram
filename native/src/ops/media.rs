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

use grammers_client::media::Uploaded;
use grammers_client::message::InputMessage;
use grammers_client::tl;
use serde::Deserialize;

use super::markup::reply_markup_from_spec;
use super::Handler;
use crate::client::{resolve_peer, resolve_upload, NativeClient};
use crate::dto::message_dto;
use crate::error::{error, invocation_error, json_string, parse_payload};
use crate::payload::{file_source, MarkupSpec, PeerTarget};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SendMediaPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    /// The local file to upload and attach, when the send names a path rather than a handle.
    path: Option<String>,
    /// `photo`, `document` (the default), `file`, which is grammers' `force_file` document, or
    /// `video`, which Telegram streams in place.
    kind: Option<String>,
    caption: Option<String>,
    /// `html`, `markdown` or `none` (the default): how the caption is parsed into entities.
    parse_mode: Option<String>,
    spoiler: Option<bool>,
    mime_type: Option<String>,
    /// Video duration in seconds; only meaningful when [Self::kind] is `video`.
    duration_seconds: Option<f64>,
    /// Video width in pixels; only meaningful when [Self::kind] is `video`.
    width: Option<i32>,
    /// Video height in pixels; only meaningful when [Self::kind] is `video`.
    height: Option<i32>,
    ttl_seconds: Option<i32>,
    invert_media: Option<bool>,
    silent: Option<bool>,
    reply_to_message_id: Option<i32>,
    /// Epoch milliseconds at which to schedule the message.
    schedule_date: Option<i64>,
    schedule_once_online: Option<bool>,
    markup: Option<MarkupSpec>,
    /// The handle of an upload that already ran, as an alternative to [Self::path]. Exactly one of
    /// the two must be set.
    file_handle: Option<i64>,
    /// A progress slot from `uploadProgressBegin` to count a path upload into, when the caller
    /// wants to observe it. Ignored for a [Self::file_handle], which uploads nothing.
    progress_handle: Option<i64>,
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
    markup: Option<MarkupSpec>,
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
    markup: Option<MarkupSpec>,
}

/// The media an edit replaces the message's media with.
///
/// Exactly one of [path], [url] or [copy_of] is set: a local file is uploaded, a URL is handed to
/// Telegram to download, and `copyOf` reuses the media of an existing message without a
/// re-upload. [kind] is `photo`, `document` (the default), `file` or `video`, as on the send side;
/// the video metadata applies to a `video` upload.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct EditMediaSpec {
    pub(crate) path: Option<String>,
    pub(crate) kind: Option<String>,
    pub(crate) url: Option<String>,
    pub(crate) copy_of: Option<CopyOfSpec>,
    /// Video duration in seconds; only meaningful for a `video` upload.
    pub(crate) duration_seconds: Option<f64>,
    /// Video width in pixels; only meaningful for a `video` upload.
    pub(crate) width: Option<i32>,
    /// Video height in pixels; only meaningful for a `video` upload.
    pub(crate) height: Option<i32>,
}

/// The message whose media an edit reuses, named the way the copy operation names its source.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct CopyOfSpec {
    pub(crate) peer: PeerTarget,
    pub(crate) message_id: i32,
}

/// The one source an [EditMediaSpec] names, after the exactly-one check.
enum EditMediaSource<'a> {
    Path(&'a str, Option<&'a str>),
    Url(&'a str, Option<&'a str>),
    Copy(&'a CopyOfSpec),
}

/// Picks the source an edit media names, refusing a spec that sets none or more than one.
fn edit_media_source(spec: &EditMediaSpec) -> Result<EditMediaSource<'_>, String> {
    match (&spec.path, &spec.url, &spec.copy_of) {
        (Some(path), None, None) => Ok(EditMediaSource::Path(path, spec.kind.as_deref())),
        (None, Some(url), None) => Ok(EditMediaSource::Url(url, spec.kind.as_deref())),
        (None, None, Some(copy)) => Ok(EditMediaSource::Copy(copy)),
        _ => Err("the edit media must set exactly one of path, url or copyOf".to_owned()),
    }
}

/// Attaches the media an edit replaces the message's media with, the same way the send-side
/// builders do: a local file is uploaded, a URL is handed to Telegram, and `copyOf` reuses an
/// existing message's media without a re-upload.
pub(crate) async fn apply_edit_media(
    native: &NativeClient,
    message: InputMessage,
    spec: &EditMediaSpec,
) -> Result<InputMessage, String> {
    match edit_media_source(spec)? {
        EditMediaSource::Path(path, kind) => {
            let kind = media_kind(kind)?;
            let uploaded = native
                .client
                .upload_file(PathBuf::from(path))
                .await
                .map_err(error)?;
            let name = uploaded_name(&uploaded);
            Ok(match kind {
                MediaKind::Photo => message.photo(uploaded),
                MediaKind::File => message.file(uploaded),
                MediaKind::Document => message.document(uploaded),
                // grammers' builders cannot carry the video attributes, so this goes raw.
                MediaKind::Video => message.media(raw_uploaded_media(
                    kind,
                    uploaded,
                    &name,
                    false,
                    None,
                    None,
                    spec.duration_seconds.unwrap_or(0.0),
                    spec.width.unwrap_or(0),
                    spec.height.unwrap_or(0),
                )),
            })
        }
        EditMediaSource::Url(url, kind) => {
            // A URL attachment is a photo or a document; a `file` kind is the generic document.
            let kind = media_kind(kind)?;
            Ok(if kind == MediaKind::Photo {
                message.photo_url(url.to_owned())
            } else {
                message.document_url(url.to_owned())
            })
        }
        EditMediaSource::Copy(copy) => {
            let source = resolve_peer(native, &copy.peer).await?;
            let messages = native
                .client
                .get_messages_by_id(source, &[copy.message_id])
                .await
                .map_err(invocation_error)?;
            let source_message = messages.into_iter().flatten().next().ok_or_else(|| {
                format!(
                    "message {} was not found in the source chat",
                    copy.message_id
                )
            })?;
            let media = source_message
                .media()
                .ok_or_else(|| "the source message carries no media to copy".to_owned())?;
            if media.to_raw_input_media().is_none() {
                return Err("the source message's media cannot be copied".to_owned());
            }
            Ok(message.copy_media(&media))
        }
    }
}

/// Builds the URL media an inline edit attaches, which is a photo or a document and never a local
/// upload: an inline message has no file to upload.
pub(crate) fn external_url_media(
    kind: Option<&str>,
    url: &str,
) -> Result<tl::enums::InputMedia, String> {
    Ok(raw_external_media(media_kind(kind)?, url, false, None))
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
    let uploaded = native.runtime.block_on(resolve_upload(
        native,
        file_source(data.path, data.file_handle)?,
        match data.progress_handle {
            Some(handle) => Some(native.uploads.progress(handle)?),
            None => None,
        },
    ))?;
    // grammers names the upload after the file it read, so a handle keeps the name it was created
    // with and a path keeps the file's own name; the raw spoiler media needs that name either way.
    let name = uploaded_name(&uploaded);

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
    // A video always goes raw, since only the raw path can add `DocumentAttributeVideo`.
    let spoiler = data.spoiler.unwrap_or(false);
    let mut message = match kind {
        MediaKind::Photo if !spoiler => message.photo(uploaded),
        MediaKind::File if !spoiler => message.file(uploaded),
        MediaKind::Document if !spoiler => message.document(uploaded),
        _ => message.media(raw_uploaded_media(
            kind,
            uploaded,
            &name,
            spoiler,
            data.mime_type.as_deref(),
            data.ttl_seconds,
            data.duration_seconds.unwrap_or(0.0),
            data.width.unwrap_or(0),
            data.height.unwrap_or(0),
        )),
    };
    if let Some(markup) = &data.markup {
        message = message.reply_markup(reply_markup_from_spec(markup)?);
    }

    let message = native
        .runtime
        .block_on(native.client.send_message(peer, message))
        .map_err(invocation_error)?;
    json_string(message_dto(native, &message))
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
    let mut message = if data.spoiler.unwrap_or(false) {
        message.media(raw_external_media(kind, &data.url, true, data.ttl_seconds))
    } else if kind == MediaKind::Photo {
        message.photo_url(data.url)
    } else {
        message.document_url(data.url)
    };
    if let Some(markup) = &data.markup {
        message = message.reply_markup(reply_markup_from_spec(markup)?);
    }

    let message = native
        .runtime
        .block_on(native.client.send_message(peer, message))
        .map_err(invocation_error)?;
    json_string(message_dto(native, &message))
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

    let mut message = message_options(
        data.caption,
        data.parse_mode.as_deref(),
        data.silent,
        data.reply_to_message_id,
        None,
        None,
        None,
    )?;
    message = message.copy_media(&media);
    if let Some(markup) = &data.markup {
        message = message.reply_markup(reply_markup_from_spec(markup)?);
    }
    let message = native
        .runtime
        .block_on(native.client.send_message(destination, message))
        .map_err(invocation_error)?;
    json_string(message_dto(native, &message))
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
    /// Telegram streams the video in place; a document carrying `DocumentAttributeVideo`.
    Video,
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
        Some("video") => Ok(MediaKind::Video),
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

/// The name an [`Uploaded`] carries, which is what a send that reuses a handle declares.
fn uploaded_name(uploaded: &Uploaded) -> String {
    match &uploaded.raw {
        tl::enums::InputFile::File(file) => file.name.clone(),
        tl::enums::InputFile::Big(file) => file.name.clone(),
        _ => String::new(),
    }
}

/// Builds the raw uploaded media the `spoiler` flag requires, matching what grammers' own
/// `photo`/`document`/`file` builders produce otherwise. A video has no builder that adds
/// `DocumentAttributeVideo`, so it is always built here.
fn raw_uploaded_media(
    kind: MediaKind,
    uploaded: Uploaded,
    file_name: &str,
    spoiler: bool,
    mime_type: Option<&str>,
    ttl_seconds: Option<i32>,
    duration_seconds: f64,
    width: i32,
    height: i32,
) -> tl::enums::InputMedia {
    match kind {
        MediaKind::Photo => tl::types::InputMediaUploadedPhoto {
            spoiler,
            live_photo: false,
            file: uploaded.raw,
            stickers: None,
            ttl_seconds,
            video: None,
        }
        .into(),
        MediaKind::Video => tl::types::InputMediaUploadedDocument {
            nosound_video: false,
            force_file: false,
            spoiler,
            file: uploaded.raw,
            thumb: None,
            mime_type: mime_type
                .map(ToOwned::to_owned)
                .unwrap_or_else(|| inferred_mime(file_name)),
            attributes: vec![
                tl::types::DocumentAttributeVideo {
                    round_message: false,
                    supports_streaming: true,
                    nosound: false,
                    duration: duration_seconds,
                    w: width,
                    h: height,
                    preload_prefix_size: None,
                    video_start_ts: None,
                    video_codec: None,
                }
                .into(),
                tl::types::DocumentAttributeFilename {
                    file_name: file_name.to_owned(),
                }
                .into(),
            ],
            stickers: None,
            ttl_seconds,
            video_cover: None,
            video_timestamp: None,
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
        MediaKind::Document | MediaKind::File | MediaKind::Video => {
            tl::types::InputMediaDocumentExternal {
                spoiler,
                url: url.to_owned(),
                ttl_seconds,
                video_cover: None,
                video_timestamp: None,
            }
            .into()
        }
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

    use std::time::{Duration, UNIX_EPOCH};

    use grammers_client::media::Uploaded;
    use grammers_client::tl;
    use serde_json::json;

    use crate::error::parse_payload;
    use crate::payload::MarkupSpec;

    use super::{
        caption_mode, edit_media_source, external_url_media, inferred_mime, media_kind,
        raw_external_media, raw_uploaded_media, schedule_time, uploaded_name, CaptionMode,
        CopyMediaPayload, EditMediaSpec, MediaKind, SendMediaPayload, SendMediaUrlPayload,
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
                    tl::enums::DocumentAttribute::Video(video) => json!({
                        "kind": "video",
                        "roundMessage": video.round_message,
                        "supportsStreaming": video.supports_streaming,
                        "nosound": video.nosound,
                        "duration": video.duration,
                        "w": video.w,
                        "h": video.h,
                        "preloadPrefixSize": video.preload_prefix_size,
                        "videoStartTs": video.video_start_ts,
                        "videoCodec": video.video_codec,
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
    fn a_media_kind_names_every_grammers_builder() {
        assert_eq!(media_kind(None), Ok(MediaKind::Document));
        assert_eq!(media_kind(Some("")), Ok(MediaKind::Document));
        assert_eq!(media_kind(Some("document")), Ok(MediaKind::Document));
        assert_eq!(media_kind(Some("photo")), Ok(MediaKind::Photo));
        assert_eq!(media_kind(Some("file")), Ok(MediaKind::File));
        assert_eq!(media_kind(Some("video")), Ok(MediaKind::Video));
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
            0.0,
            0,
            0,
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
    fn the_uploaded_name_comes_from_the_file_grammers_produced() {
        assert_eq!(uploaded_name(&uploaded()), "report.pdf");
        let big = Uploaded::from_raw(tl::enums::InputFile::Big(tl::types::InputFileBig {
            id: 8,
            parts: 20,
            name: "movie.mp4".to_owned(),
        }));
        assert_eq!(uploaded_name(&big), "movie.mp4");
    }

    #[test]
    fn the_send_media_payload_reads_an_upload_handle_instead_of_a_path() {
        let data: SendMediaPayload = parse_payload(
            &json!({ "peerHandle": 12, "fileHandle": 7, "kind": "photo" }).to_string(),
        )
        .expect("the payload decodes");

        assert_eq!(data.file_handle, Some(7));
        assert_eq!(data.path, None);
        assert_eq!(data.kind.as_deref(), Some("photo"));
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
            0.0,
            0,
            0,
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
    fn a_video_document_streams_in_place_with_its_dimensions() {
        let media = raw_uploaded_media(
            MediaKind::Video,
            uploaded(),
            "movie.mp4",
            false,
            None,
            Some(120),
            12.5,
            1920,
            1080,
        );

        assert_eq!(
            media_json(&media),
            json!({
                "kind": "uploadedDocument",
                "nosoundVideo": false,
                "forceFile": false,
                "spoiler": false,
                "file": { "kind": "file", "id": 4242, "parts": 1, "name": "report.pdf" },
                "mimeType": "video/mp4",
                "attributes": [
                    {
                        "kind": "video",
                        "roundMessage": false,
                        "supportsStreaming": true,
                        "nosound": false,
                        "duration": 12.5,
                        "w": 1920,
                        "h": 1080,
                        "preloadPrefixSize": null,
                        "videoStartTs": null,
                        "videoCodec": null,
                    },
                    { "kind": "filename", "fileName": "movie.mp4" },
                ],
                "ttlSeconds": 120,
            })
        );
    }

    #[test]
    fn a_spoiler_photo_carries_no_name_or_mime() {
        let media = raw_uploaded_media(
            MediaKind::Photo,
            uploaded(),
            "x.png",
            true,
            None,
            Some(7),
            0.0,
            0,
            0,
        );

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
    fn the_mime_type_is_inferred_from_the_extension() {
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
                "durationSeconds": 12.5,
                "width": 1920,
                "height": 1080,
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
        assert_eq!(data.path.as_deref(), Some("/tmp/report.pdf"));
        assert_eq!(data.kind.as_deref(), Some("file"));
        assert_eq!(data.caption.as_deref(), Some("<b>hi</b>"));
        assert_eq!(data.parse_mode.as_deref(), Some("html"));
        assert_eq!(data.spoiler, Some(true));
        assert_eq!(data.mime_type.as_deref(), Some("application/pdf"));
        assert_eq!(data.duration_seconds, Some(12.5));
        assert_eq!(data.width, Some(1920));
        assert_eq!(data.height, Some(1080));
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
        assert_eq!(data.duration_seconds, None);
        assert_eq!(data.width, None);
        assert_eq!(data.height, None);
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

    #[test]
    fn the_media_payloads_decode_a_markup_spec() {
        let data: SendMediaPayload = parse_payload(
            &json!({
                "peerHandle": 1,
                "path": "/tmp/a.pdf",
                "markup": {
                    "kind": "inline",
                    "rows": [[{ "type": "url", "text": "Docs", "url": "https://example.org" }]],
                },
            })
            .to_string(),
        )
        .expect("the payload decodes");
        assert!(matches!(data.markup, Some(MarkupSpec::Inline { .. })));

        let data: SendMediaUrlPayload = parse_payload(
            &json!({
                "peerHandle": 1,
                "url": "https://example.org/a.jpg",
                "markup": { "kind": "keyboard", "rows": [[{ "type": "text", "text": "Go" }]] },
            })
            .to_string(),
        )
        .expect("the payload decodes");
        assert!(matches!(data.markup, Some(MarkupSpec::Keyboard { .. })));

        let data: CopyMediaPayload = parse_payload(
            &json!({
                "destination": { "peerHandle": 1 },
                "source": { "peerHandle": 2 },
                "messageId": 31,
                "markup": { "kind": "hide" },
            })
            .to_string(),
        )
        .expect("the payload decodes");
        assert!(matches!(data.markup, Some(MarkupSpec::Hide { .. })));

        // An absent markup is `None`, so a send without one is unchanged.
        let data: SendMediaPayload =
            parse_payload(&json!({ "peerHandle": 1, "path": "/tmp/a.pdf" }).to_string())
                .expect("the payload decodes");
        assert!(data.markup.is_none());
    }

    #[test]
    fn the_edit_media_spec_decodes_each_source() {
        let upload: EditMediaSpec =
            parse_payload(&json!({ "path": "/tmp/a.pdf", "kind": "file" }).to_string())
                .expect("an upload media");
        assert_eq!(upload.path.as_deref(), Some("/tmp/a.pdf"));
        assert_eq!(upload.kind.as_deref(), Some("file"));
        assert!(upload.url.is_none() && upload.copy_of.is_none());

        let url: EditMediaSpec = parse_payload(
            &json!({ "url": "https://example.org/a.jpg", "kind": "photo" }).to_string(),
        )
        .expect("a url media");
        assert_eq!(url.url.as_deref(), Some("https://example.org/a.jpg"));
        assert_eq!(url.kind.as_deref(), Some("photo"));

        let copy: EditMediaSpec = parse_payload(
            &json!({
                "copyOf": { "peer": { "username": "channel" }, "messageId": 31 },
            })
            .to_string(),
        )
        .expect("a copy media");
        let copy_of = copy.copy_of.expect("a source");
        assert_eq!(copy_of.peer.username.as_deref(), Some("channel"));
        assert_eq!(copy_of.message_id, 31);

        let video: EditMediaSpec = parse_payload(
            &json!({
                "path": "/tmp/movie.mp4",
                "kind": "video",
                "durationSeconds": 12.5,
                "width": 1920,
                "height": 1080,
            })
            .to_string(),
        )
        .expect("a video media");
        assert_eq!(video.kind.as_deref(), Some("video"));
        assert_eq!(video.duration_seconds, Some(12.5));
        assert_eq!(video.width, Some(1920));
        assert_eq!(video.height, Some(1080));
    }

    #[test]
    fn the_edit_media_source_must_be_the_only_one_set() {
        let upload: EditMediaSpec =
            parse_payload(&json!({ "path": "/tmp/a.pdf" }).to_string()).expect("the payload");
        assert!(matches!(
            edit_media_source(&upload),
            Ok(super::EditMediaSource::Path("/tmp/a.pdf", None))
        ));

        let none: EditMediaSpec =
            parse_payload(&json!({ "kind": "photo" }).to_string()).expect("the payload");
        assert_eq!(
            edit_media_source(&none).err(),
            Some("the edit media must set exactly one of path, url or copyOf".to_owned())
        );

        let both: EditMediaSpec = parse_payload(
            &json!({ "path": "/tmp/a.pdf", "url": "https://example.org/a.jpg" }).to_string(),
        )
        .expect("the payload");
        assert_eq!(
            edit_media_source(&both).err(),
            Some("the edit media must set exactly one of path, url or copyOf".to_owned())
        );
    }

    #[test]
    fn an_inline_edit_media_is_a_url_photo_or_document() {
        let photo =
            external_url_media(Some("photo"), "https://example.org/a.jpg").expect("a photo media");
        assert_eq!(
            media_json(&photo),
            json!({
                "kind": "photoExternal",
                "spoiler": false,
                "url": "https://example.org/a.jpg",
                "ttlSeconds": null,
            })
        );

        // An absent kind is grammers' document, and `file` is the same external document.
        let document = external_url_media(None, "https://example.org/a.pdf").expect("a document");
        assert_eq!(media_json(&document)["kind"], json!("documentExternal"));

        assert_eq!(
            external_url_media(Some("sticker"), "https://example.org/a").unwrap_err(),
            "unsupported media kind: sticker"
        );
    }
}
