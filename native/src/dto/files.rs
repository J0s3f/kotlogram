//! File-transfer projections: a finished download, one streamed chunk, the metadata of an upload
//! and a profile photo.
//!
//! The chunk bytes have no JSON representation, so they travel base64-encoded, the same convention
//! [`crate::dto::update`] uses for the TL bytes of a raw update. The encoding is the one
//! `java.util.Base64.getDecoder()` reads back on the other side of the JNI boundary, and the bridge
//! may not take a dependency to get it, so it is written out here.

use grammers_client::grammers_tl_types as tl;
use grammers_client::types::media::Uploaded;
use grammers_client::types::photo_sizes::{PhotoSize, VecExt};
use grammers_client::types::Photo;
use serde::Serialize;

/// Where a `downloadMedia` wrote the file, and how many bytes it holds.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct DownloadResultDto {
    pub(crate) path: String,
    pub(crate) size: i64,
}

/// One chunk of a streamed download, as `downloadMediaChunk` answers it.
///
/// [Self::data] is the chunk's bytes, base64 because JSON has no byte string; [Self::size] is their
/// length once decoded and [Self::offset] is where in the file the chunk starts.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct MediaChunkDto {
    pub(crate) data: String,
    pub(crate) offset: i64,
    pub(crate) size: i64,
}

/// The metadata grammers keeps for an upload, which a later send can reuse.
///
/// grammers exposes no size on an upload, so [Self::size] is the length of the input that was
/// uploaded, and [Self::is_big] distinguishes the two TL constructors grammers chooses between at
/// ten megabytes. Only the small-file constructor carries an MD5 checksum.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct UploadedFileDto {
    pub(crate) id: i64,
    pub(crate) name: String,
    pub(crate) size: i64,
    pub(crate) parts: i32,
    pub(crate) md5_checksum: Option<String>,
    pub(crate) is_big: bool,
}

/// A profile photo of a peer.
///
/// grammers exposes no data-centre accessor on a photo, so [Self::dc_id] is read off the raw
/// payload and is `null` for the empty constructor, which carries no location. [Self::size] is the
/// byte size of the largest thumbnail, and [Self::width] and [Self::height] are that thumbnail's
/// pixel dimensions, `null` when it describes bytes without a resolution.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ProfilePhotoDto {
    pub(crate) id: i64,
    pub(crate) dc_id: Option<i32>,
    pub(crate) size: i64,
    pub(crate) width: Option<i32>,
    pub(crate) height: Option<i32>,
    pub(crate) spoiler: bool,
    pub(crate) ttl_seconds: Option<i32>,
}

/// Projects the metadata of an uploaded file, given the length of the input that was uploaded.
///
/// `Client::upload_file` only ever produces the small or the big constructor, so anything else
/// means the value did not come from an upload and is reported rather than guessed at.
pub(crate) fn uploaded_dto(uploaded: &Uploaded, size: i64) -> Result<UploadedFileDto, String> {
    Ok(match &uploaded.raw {
        tl::enums::InputFile::File(file) => UploadedFileDto {
            id: file.id,
            name: file.name.clone(),
            size,
            parts: file.parts,
            md5_checksum: Some(file.md5_checksum.clone()),
            is_big: false,
        },
        tl::enums::InputFile::Big(file) => UploadedFileDto {
            id: file.id,
            name: file.name.clone(),
            size,
            parts: file.parts,
            md5_checksum: None,
            is_big: true,
        },
        _ => return Err("the upload is not a file grammers produced".to_owned()),
    })
}

/// Projects a profile photo, which `iter_profile_photos` yields.
pub(crate) fn profile_photo_dto(photo: &Photo) -> ProfilePhotoDto {
    let (width, height) = dimensions(photo.thumbs().largest());
    ProfilePhotoDto {
        id: photo.id(),
        dc_id: photo_dc_id(photo),
        size: photo.size(),
        width,
        height,
        spoiler: photo.is_spoiler(),
        ttl_seconds: photo.ttl_seconds(),
    }
}

/// The data centre a photo lives in, which only the full constructor names.
fn photo_dc_id(photo: &Photo) -> Option<i32> {
    match &photo.raw.photo {
        Some(tl::enums::Photo::Photo(inner)) => Some(inner.dc_id),
        _ => None,
    }
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

/// Encodes [bytes] as base64, which is how a byte string travels through a JSON document.
///
/// The encoding is the alphabet and padding of RFC 4648 that `java.util.Base64.getDecoder()` reads
/// back, written out because the bridge may not take a dependency for it.
pub(crate) fn base64(bytes: &[u8]) -> String {
    const ALPHABET: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut encoded = String::with_capacity(bytes.len().div_ceil(3) * 4);
    for chunk in bytes.chunks(3) {
        // The three bytes of a chunk are one 24-bit group, of which the last one may be missing;
        // the bits that are not there are written as zero and cut off again by the padding.
        let group = chunk.iter().enumerate().fold(0u32, |group, (index, byte)| {
            group | u32::from(*byte) << (16 - 8 * index)
        });
        for index in 0..=chunk.len() {
            let shift = 18 - 6 * index;
            encoded.push(char::from(ALPHABET[((group >> shift) & 0x3f) as usize]));
            if index == chunk.len() {
                // A short chunk is padded to a four-character group.
                for _ in chunk.len()..3 {
                    encoded.push('=');
                }
                break;
            }
        }
    }
    encoded
}

#[cfg(test)]
mod tests {
    use serde_json::json;

    use super::*;

    /// Asserts that [dto] encodes to exactly [expected].
    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    #[test]
    fn a_download_result_reports_the_path_and_size() {
        assert_json(
            &DownloadResultDto {
                path: "/tmp/holidays.jpg".to_owned(),
                size: 5_150,
            },
            json!({"path": "/tmp/holidays.jpg", "size": 5_150}),
        );
    }

    #[test]
    fn a_chunk_encodes_its_bytes_as_base64() {
        assert_json(
            &MediaChunkDto {
                data: "Zm9vYmFy".to_owned(),
                offset: 524_288,
                size: 6,
            },
            json!({"data": "Zm9vYmFy", "offset": 524_288, "size": 6}),
        );
    }

    #[test]
    fn an_uploaded_small_file_carries_its_checksum() {
        let uploaded = Uploaded {
            raw: tl::enums::InputFile::File(tl::types::InputFile {
                id: 7,
                parts: 2,
                name: "holidays.jpg".to_owned(),
                md5_checksum: "d41d8cd98f00b204e9800998ecf8427e".to_owned(),
            }),
        };
        assert_json(
            &uploaded_dto(&uploaded, 1_048_576).expect("a small upload projects"),
            json!({
                "id": 7,
                "name": "holidays.jpg",
                "size": 1_048_576,
                "parts": 2,
                "md5Checksum": "d41d8cd98f00b204e9800998ecf8427e",
                "isBig": false,
            }),
        );
    }

    #[test]
    fn an_uploaded_big_file_has_no_checksum() {
        let uploaded = Uploaded {
            raw: tl::enums::InputFile::Big(tl::types::InputFileBig {
                id: 8,
                parts: 20,
                name: "movie.mp4".to_owned(),
            }),
        };
        assert_json(
            &uploaded_dto(&uploaded, 10_485_760).expect("a big upload projects"),
            json!({
                "id": 8,
                "name": "movie.mp4",
                "size": 10_485_760,
                "parts": 20,
                "md5Checksum": null,
                "isBig": true,
            }),
        );
    }

    #[test]
    fn a_profile_photo_declares_exactly_the_projected_fields() {
        let mut names: Vec<String> = serde_json::to_value(ProfilePhotoDto {
            id: 1,
            dc_id: None,
            size: 0,
            width: None,
            height: None,
            spoiler: false,
            ttl_seconds: None,
        })
        .expect("a projection encodes")
        .as_object()
        .expect("an object")
        .keys()
        .cloned()
        .collect();
        names.sort();
        assert_eq!(
            names,
            [
                "dcId",
                "height",
                "id",
                "size",
                "spoiler",
                "ttlSeconds",
                "width"
            ]
        );
    }

    #[test]
    fn base64_matches_the_reference_vectors() {
        // The test vectors of RFC 4648, which is what `java.util.Base64` reads and writes.
        for (bytes, expected) in [
            (&b""[..], ""),
            (b"f", "Zg=="),
            (b"fo", "Zm8="),
            (b"foo", "Zm9v"),
            (b"foob", "Zm9vYg=="),
            (b"fooba", "Zm9vYmE="),
            (b"foobar", "Zm9vYmFy"),
        ] {
            assert_eq!(
                base64(bytes),
                expected,
                "{bytes:?} does not encode as {expected}"
            );
        }
        // A payload of every byte value, which is what a chunk can be.
        let every: Vec<u8> = (0..=u8::MAX).collect();
        assert_eq!(base64(&every), "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+/w==");
    }
}
