//! File transfer: downloading media, chunked downloads, uploads with declared MIME types and profile photos.

use std::io::Cursor;
use std::path::PathBuf;
use std::sync::atomic::Ordering;

use grammers_client::media::Media as ClientMedia;
use grammers_client::media::Uploaded;
use grammers_session::types::PeerRef;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, upload_path_with_progress, NativeClient, StreamUpload};
use crate::dto::files::{
    base64, decode_base64, profile_photo_dto, uploaded_dto, DownloadResultDto, MediaChunkDto,
};
use crate::error::{error, invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// grammers' own chunk bounds, in bytes. `DownloadIter::chunk_size` panics outside them, so they are
/// checked here rather than handed to it.
const MIN_CHUNK_SIZE: i32 = 4 * 1024;
const MAX_CHUNK_SIZE: i32 = 512 * 1024;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct DownloadMediaPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
    path: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct DownloadChunkPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    message_id: i32,
    chunk_size: i32,
    skip_chunks: i32,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UploadFilePayload {
    path: String,
    /// A progress slot from `uploadProgressBegin` to count this upload into, when the caller wants
    /// to observe it. Absent keeps the plain grammers path upload.
    progress_handle: Option<i64>,
}

/// Payload of `uploadBytes`: the declared name and the whole file as one base64 string.
///
/// Base64 inflates the data by a third and JSON carries it as text, so this is for small files.
/// `uploadStreamBegin`/`Chunk`/`Finish` is the operation to use for anything sizeable.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UploadBytesPayload {
    name: String,
    data_base64: String,
}

/// Payload of `uploadStreamBegin`: the name the finished upload will carry and its total size.
///
/// grammers has to know the size before it can read the first part (it decides small versus big
/// file and the part count from it), so a stream that wants live progress declares the size up
/// front rather than accumulating the bytes to measure them at the end.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UploadStreamBeginPayload {
    name: String,
    size: u64,
}

/// Payload of `uploadProgressBegin`: the total an upload will have, which may be corrected at
/// upload time when a path is opened.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UploadProgressBeginPayload {
    total: u64,
}

/// Payload of `uploadProgress`: the progress slot to read.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UploadProgressPayload {
    upload_id: i64,
}

/// Payload of `uploadStreamChunk`: the stream to append to and one base64 chunk.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UploadStreamChunkPayload {
    upload_id: i64,
    data_base64: String,
}

/// Payload of `uploadStreamFinish`: the stream whose accumulated bytes are uploaded.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UploadStreamFinishPayload {
    upload_id: i64,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ProfilePhotosPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    limit: Option<usize>,
}

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "downloadMedia",
    "downloadMediaChunk",
    "uploadFile",
    "uploadBytes",
    "uploadProgress",
    "uploadProgressBegin",
    "uploadStreamBegin",
    "uploadStreamChunk",
    "uploadStreamFinish",
    "iterProfilePhotos",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "downloadMedia" => download_media,
        "downloadMediaChunk" => download_media_chunk,
        "uploadFile" => upload_file,
        "uploadBytes" => upload_bytes,
        "uploadProgress" => upload_progress,
        "uploadProgressBegin" => upload_progress_begin,
        "uploadStreamBegin" => upload_stream_begin,
        "uploadStreamChunk" => upload_stream_chunk,
        "uploadStreamFinish" => upload_stream_finish,
        "iterProfilePhotos" => iter_profile_photos,
        _ => return None,
    })
}

fn download_media(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: DownloadMediaPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let media = message_media(native, peer, data.message_id)?;
    let path = PathBuf::from(&data.path);
    native
        .runtime
        .block_on(native.client.download_media(&media, &path))
        .map_err(error)?;
    let size = std::fs::metadata(&path)
        .map(|metadata| metadata.len() as i64)
        .map_err(error)?;
    json_string(DownloadResultDto {
        path: data.path,
        size,
    })
}

fn download_media_chunk(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: DownloadChunkPayload = parse_payload(payload)?;
    if data.skip_chunks < 0 {
        return Err("skipChunks must not be negative".to_owned());
    }
    let chunk_size = validate_chunk_size(data.chunk_size)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let media = message_media(native, peer, data.message_id)?;
    let chunk = native.runtime.block_on(async {
        let mut download = native
            .client
            .iter_download(&media)
            .chunk_size(chunk_size)
            .skip_chunks(data.skip_chunks);
        download.next().await.map_err(invocation_error)
    })?;
    match chunk {
        // The iterator reports the end of the file as `None`, which the bridge answers as `null`;
        // the Kotlin side requests a nullable chunk for exactly that.
        None => json_string(json!(null)),
        Some(bytes) => json_string(MediaChunkDto {
            data: base64(&bytes),
            offset: chunk_offset(chunk_size, data.skip_chunks),
            size: bytes.len() as i64,
        }),
    }
}

/// Uploads a local file, counting it into a `uploadProgressBegin` slot when one is named.
///
/// Without a slot, grammers' own `Client::upload_file` runs; with one, the same upload runs over a
/// counting reader, so the bytes and the resulting metadata are unchanged and only the observation
/// point is added.
fn upload_file(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UploadFilePayload = parse_payload(payload)?;
    let path = PathBuf::from(&data.path);
    let size = std::fs::metadata(&path)
        .map(|metadata| metadata.len() as i64)
        .map_err(error)?;
    let uploaded = match data.progress_handle {
        Some(handle) => {
            let progress = native.uploads.progress(handle)?;
            native.runtime.block_on(upload_path_with_progress(
                &native.client,
                path,
                progress,
            ))?
        }
        None => native
            .runtime
            .block_on(native.client.upload_file(&path))
            .map_err(error)?,
    };
    finish_upload(native, uploaded, size)
}

/// Uploads [data] under [name] in one shot: the single-shot sibling of the streamed path.
///
/// The bytes are carried base64 in the JSON payload, which inflates them by a third; a caller with
/// anything but a small file should use the `uploadStream*` operations instead.
fn upload_bytes(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UploadBytesPayload = parse_payload(payload)?;
    let bytes = decode_base64(&data.data_base64)?;
    let size = bytes.len() as i64;
    let mut stream = Cursor::new(bytes);
    let uploaded = native
        .runtime
        .block_on(
            native
                .client
                .upload_stream(&mut stream, size as usize, data.name),
        )
        .map_err(error)?;
    finish_upload(native, uploaded, size)
}

/// Opens a chunked upload and returns the id every later chunk and the progress poll name.
///
/// The upload task is spawned here and reads the channel the chunks are sent to, so the bytes are
/// on their way to Telegram while the JVM is still reading its input.
fn upload_stream_begin(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UploadStreamBeginPayload = parse_payload(payload)?;
    let upload_id = native.start_stream_upload(data.name, data.size)?;
    json_string(json!({ "uploadId": upload_id }))
}

/// Hands one decoded chunk to the stream [UploadStreamChunkPayload::upload_id] names.
///
/// The send awaits room in the stream's bounded channel, which is what keeps a fast producer from
/// piling the whole file up in memory.
fn upload_stream_chunk(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UploadStreamChunkPayload = parse_payload(payload)?;
    let bytes = decode_base64(&data.data_base64)?;
    native.append_stream(data.upload_id, &bytes)?;
    json_string(json!({ "ok": true }))
}

/// Ends a stream's input and waits for the upload task that has been consuming it.
///
/// Dropping the producing end of the channel is what the reader sees as end-of-file. A stream that
/// received fewer bytes than it declared cannot satisfy grammers, so it is refused before the task
/// is awaited.
fn upload_stream_finish(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UploadStreamFinishPayload = parse_payload(payload)?;
    let stream = native.uploads.take_stream(data.upload_id)?;
    let StreamUpload {
        size,
        accepted,
        sender,
        task,
        ..
    } = stream;
    drop(sender);
    let received = accepted.load(Ordering::Relaxed);
    if received != size {
        task.abort();
        return Err(format!(
            "upload stream {} was declared with {size} bytes but received {received}",
            data.upload_id
        ));
    }
    let uploaded = native.runtime.block_on(task).map_err(error)??;
    finish_upload(native, uploaded, size as i64)
}

/// Allocates a progress slot for an upload whose bytes are not fed through a stream, so that a
/// `uploadFile` or a path `sendMedia` can be observed while its single native call is running.
fn upload_progress_begin(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UploadProgressBeginPayload = parse_payload(payload)?;
    let upload_id = native.uploads.begin_progress(data.total)?;
    json_string(json!({ "uploadId": upload_id }))
}

/// Reads the live progress of the upload [UploadProgressPayload::upload_id] names.
///
/// This is a plain read of the counters, so a render loop can poll it from a different thread while
/// the upload's own call is blocked.
fn upload_progress(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UploadProgressPayload = parse_payload(payload)?;
    let progress = native.uploads.progress(data.upload_id)?;
    json_string(progress.snapshot())
}

/// Registers a finished upload and projects the metadata plus the handle that references it.
fn finish_upload(native: &NativeClient, uploaded: Uploaded, size: i64) -> Result<String, String> {
    let handle = native.uploads.register(uploaded.clone())?;
    json_string(uploaded_dto(&uploaded, size, Some(handle))?)
}

fn iter_profile_photos(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ProfilePhotosPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    let limit = data.limit.unwrap_or(50).clamp(1, 100);
    let photos = native.runtime.block_on(async {
        // The iterator has no limit of its own, so it is stopped once enough photos are collected.
        let mut iterator = native.client.iter_profile_photos(peer);
        let mut photos = Vec::new();
        while photos.len() < limit {
            match iterator.next().await.map_err(invocation_error)? {
                Some(photo) => photos.push(profile_photo_dto(&photo)),
                None => break,
            }
        }
        Ok::<_, String>(photos)
    })?;
    json_string(photos)
}

/// Loads the message a download names and takes its media, which is what a downloader consumes.
fn message_media(
    native: &NativeClient,
    peer: PeerRef,
    message_id: i32,
) -> Result<ClientMedia, String> {
    let messages = native
        .runtime
        .block_on(native.client.get_messages_by_id(peer, &[message_id]))
        .map_err(invocation_error)?;
    let message = messages
        .into_iter()
        .flatten()
        .next()
        .ok_or_else(|| format!("message {message_id} was not found"))?;
    message
        .media()
        .ok_or_else(|| format!("message {message_id} has no downloadable media"))
}

/// Rejects a chunk size grammers' own iterator would panic on.
fn validate_chunk_size(size: i32) -> Result<i32, String> {
    if (MIN_CHUNK_SIZE..=MAX_CHUNK_SIZE).contains(&size) && size % MIN_CHUNK_SIZE == 0 {
        Ok(size)
    } else {
        Err(format!(
            "chunkSize must be a multiple of {MIN_CHUNK_SIZE} between {MIN_CHUNK_SIZE} and \
             {MAX_CHUNK_SIZE}, got {size}"
        ))
    }
}

/// The byte offset a chunk starts at, which is the chunk size times the number of skipped chunks.
fn chunk_offset(chunk_size: i32, skip_chunks: i32) -> i64 {
    i64::from(chunk_size) * i64::from(skip_chunks)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_chunk_size_inside_the_bounds_and_divisible_is_accepted() {
        assert_eq!(validate_chunk_size(MIN_CHUNK_SIZE), Ok(MIN_CHUNK_SIZE));
        assert_eq!(validate_chunk_size(MAX_CHUNK_SIZE), Ok(MAX_CHUNK_SIZE));
        assert_eq!(validate_chunk_size(64 * 1024), Ok(64 * 1024));
    }

    #[test]
    fn a_chunk_size_grammers_would_reject_is_reported() {
        // Below the minimum, above the maximum, and not a multiple of the minimum.
        for size in [
            0,
            MIN_CHUNK_SIZE - 1,
            MAX_CHUNK_SIZE + 1,
            MIN_CHUNK_SIZE + 1,
        ] {
            assert!(
                validate_chunk_size(size).is_err(),
                "{size} should have been rejected"
            );
        }
    }

    #[test]
    fn a_chunk_offset_is_the_chunk_size_times_the_skipped_count() {
        assert_eq!(chunk_offset(MAX_CHUNK_SIZE, 0), 0);
        assert_eq!(chunk_offset(MAX_CHUNK_SIZE, 3), 1_572_864);
        assert_eq!(chunk_offset(MIN_CHUNK_SIZE, 1), 4_096);
    }

    #[test]
    fn the_download_payload_reads_a_flattened_peer_and_the_message() {
        let data: DownloadMediaPayload =
            serde_json::from_str(r#"{"peerHandle":12,"messageId":31,"path":"/tmp/a.jpg"}"#)
                .expect("the payload decodes");
        assert_eq!(data.peer.peer_handle, Some(12));
        assert_eq!(data.peer.username, None);
        assert_eq!(data.message_id, 31);
        assert_eq!(data.path, "/tmp/a.jpg");
    }

    #[test]
    fn the_chunk_payload_reads_its_bounds() {
        let data: DownloadChunkPayload = serde_json::from_str(
            r#"{"username":"channel","messageId":31,"chunkSize":524288,"skipChunks":2}"#,
        )
        .expect("the payload decodes");
        assert_eq!(data.peer.username.as_deref(), Some("channel"));
        assert_eq!(data.chunk_size, 524_288);
        assert_eq!(data.skip_chunks, 2);
    }

    #[test]
    fn the_upload_payloads_read_their_camel_case_fields() {
        let bytes: UploadBytesPayload =
            parse_payload(r#"{"name":"holidays.jpg","dataBase64":"Zm9vYmFy"}"#)
                .expect("an upload-bytes payload");
        assert_eq!(bytes.name, "holidays.jpg");
        assert_eq!(bytes.data_base64, "Zm9vYmFy");

        let file: UploadFilePayload =
            parse_payload(r#"{"path":"/tmp/a.jpg","progressHandle":12}"#).expect("an upload payload");
        assert_eq!(file.path, "/tmp/a.jpg");
        assert_eq!(file.progress_handle, Some(12));
        // A path upload with no progress slot leaves the handle unset.
        let plain: UploadFilePayload =
            parse_payload(r#"{"path":"/tmp/a.jpg"}"#).expect("a plain upload payload");
        assert_eq!(plain.progress_handle, None);

        let begin: UploadStreamBeginPayload =
            parse_payload(r#"{"name":"movie.mp4","size":1048576}"#).expect("a begin payload");
        assert_eq!(begin.name, "movie.mp4");
        assert_eq!(begin.size, 1_048_576);

        let progress_begin: UploadProgressBeginPayload =
            parse_payload(r#"{"total":2048}"#).expect("a progress-begin payload");
        assert_eq!(progress_begin.total, 2_048);

        let progress: UploadProgressPayload =
            parse_payload(r#"{"uploadId":12}"#).expect("a progress payload");
        assert_eq!(progress.upload_id, 12);

        let chunk: UploadStreamChunkPayload =
            parse_payload(r#"{"uploadId":12,"dataBase64":"Zm9v"}"#).expect("a chunk payload");
        assert_eq!(chunk.upload_id, 12);
        assert_eq!(chunk.data_base64, "Zm9v");

        let finish: UploadStreamFinishPayload =
            parse_payload(r#"{"uploadId":12}"#).expect("a finish payload");
        assert_eq!(finish.upload_id, 12);
    }
}
