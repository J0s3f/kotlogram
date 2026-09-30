//! File transfer: downloading media, chunked downloads, uploads with declared MIME types and profile photos.

use std::path::PathBuf;

use grammers_client::media::Media as ClientMedia;
use grammers_session::types::PeerRef;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::files::{
    base64, profile_photo_dto, uploaded_dto, DownloadResultDto, MediaChunkDto,
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

fn upload_file(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UploadFilePayload = parse_payload(payload)?;
    let path = PathBuf::from(&data.path);
    let size = std::fs::metadata(&path)
        .map(|metadata| metadata.len() as i64)
        .map_err(error)?;
    let uploaded = native
        .runtime
        .block_on(native.client.upload_file(&path))
        .map_err(error)?;
    json_string(uploaded_dto(&uploaded, size)?)
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
}
