//! File-transfer projections: a finished download, one streamed chunk, the metadata of an upload
//! and a profile photo.
//!
//! The chunk bytes have no JSON representation, so they travel base64-encoded, the same convention
//! [`crate::dto::update`] uses for the TL bytes of a raw update. The encoding is the one
//! `java.util.Base64.getDecoder()` reads back on the other side of the JNI boundary, and the bridge
//! may not take a dependency to get it, so it is written out here.

use grammers_client::media::Photo;
use grammers_client::media::PhotoSize;
use grammers_client::media::Uploaded;
use grammers_client::tl;
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
    /// The per-client registry handle a later send references this upload by. Distinct from
    /// [Self::id], which is the TL file id Telegram assigned: the handle lives only as long as the
    /// client and is what [`crate::client::UploadRegistry`] maps back to the uploaded file.
    pub(crate) handle: Option<i64>,
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
pub(crate) fn uploaded_dto(
    uploaded: &Uploaded,
    size: i64,
    handle: Option<i64>,
) -> Result<UploadedFileDto, String> {
    Ok(match &uploaded.raw {
        tl::enums::InputFile::File(file) => UploadedFileDto {
            id: file.id,
            name: file.name.clone(),
            size,
            parts: file.parts,
            md5_checksum: Some(file.md5_checksum.clone()),
            is_big: false,
            handle,
        },
        tl::enums::InputFile::Big(file) => UploadedFileDto {
            id: file.id,
            name: file.name.clone(),
            size,
            parts: file.parts,
            md5_checksum: None,
            is_big: true,
            handle,
        },
        _ => return Err("the upload is not a file grammers produced".to_owned()),
    })
}

/// Projects a profile photo, which `iter_profile_photos` yields.
pub(crate) fn profile_photo_dto(photo: &Photo) -> ProfilePhotoDto {
    // grammers has no "largest thumb" helper, so the largest is the one with the most bytes.
    let (width, height) = dimensions(photo.thumbs().iter().max_by_key(|thumb| thumb.size()));
    ProfilePhotoDto {
        id: photo.id(),
        dc_id: photo_dc_id(photo),
        size: photo.size().unwrap_or(0) as i64,
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

/// Decodes [text] as base64, the inverse of [`base64`] and the encoding `java.util.Base64` writes.
///
/// Whitespace is ignored, so a document may wrap the data. A character outside the alphabet, a
/// length that is not a whole number of four-character groups, or padding anywhere but the last
/// group is refused rather than guessed at; every other decoder in the bridge reports an error the
/// same way.
pub(crate) fn decode_base64(text: &str) -> Result<Vec<u8>, String> {
    let bytes: Vec<u8> = text
        .bytes()
        .filter(|byte| !byte.is_ascii_whitespace())
        .collect();
    if bytes.len() % 4 != 0 {
        return Err(format!(
            "base64 data must be a whole number of four-character groups, got {}",
            bytes.len()
        ));
    }
    let groups = bytes.len() / 4;
    let mut decoded = Vec::with_capacity(groups * 3);
    for (index, chunk) in bytes.chunks(4).enumerate() {
        let mut value = 0u32;
        let mut padding = 0usize;
        for (position, byte) in chunk.iter().enumerate() {
            if *byte == b'=' {
                // Padding is only legal in the last group, and only in its last two positions.
                if index + 1 != groups || position < 2 {
                    return Err("base64 padding may only trail the final group".to_owned());
                }
                padding += 1;
                value <<= 6;
                continue;
            }
            if padding > 0 {
                return Err("base64 data must not follow its padding".to_owned());
            }
            value = (value << 6) | u32::from(sextet(*byte)?);
        }
        // The 24-bit group holds three bytes, of which the padded ones are not produced.
        let produced = 3 - padding;
        decoded.push((value >> 16) as u8);
        if produced >= 2 {
            decoded.push((value >> 8) as u8);
        }
        if produced >= 3 {
            decoded.push(value as u8);
        }
    }
    Ok(decoded)
}

/// The value one base64 character stands for, or an error naming the offender.
fn sextet(byte: u8) -> Result<u8, String> {
    Ok(match byte {
        b'A'..=b'Z' => byte - b'A',
        b'a'..=b'z' => byte - b'a' + 26,
        b'0'..=b'9' => byte - b'0' + 52,
        b'+' => 62,
        b'/' => 63,
        other => return Err(format!("invalid base64 character: {}", char::from(other))),
    })
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
            &uploaded_dto(&uploaded, 1_048_576, Some(7)).expect("a small upload projects"),
            json!({
                "id": 7,
                "name": "holidays.jpg",
                "size": 1_048_576,
                "parts": 2,
                "md5Checksum": "d41d8cd98f00b204e9800998ecf8427e",
                "isBig": false,
                "handle": 7,
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
            &uploaded_dto(&uploaded, 10_485_760, None).expect("a big upload projects"),
            json!({
                "id": 8,
                "name": "movie.mp4",
                "size": 10_485_760,
                "parts": 20,
                "md5Checksum": null,
                "isBig": true,
                "handle": null,
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

    #[test]
    fn base64_decoding_inverts_the_reference_vectors() {
        for (encoded, expected) in [
            ("", &b""[..]),
            ("Zg==", b"f"),
            ("Zm8=", b"fo"),
            ("Zm9v", b"foo"),
            ("Zm9vYg==", b"foob"),
            ("Zm9vYmE=", b"fooba"),
            ("Zm9vYmFy", b"foobar"),
        ] {
            assert_eq!(
                decode_base64(encoded),
                Ok(expected.to_vec()),
                "{encoded} must decode to {expected:?}"
            );
        }
        // Whitespace is ignored, so a wrapped document decodes the same as a compact one.
        assert_eq!(decode_base64("Zm9v\n YmFy"), Ok(b"foobar".to_vec()));
        // Every byte value survives a full round trip.
        let every: Vec<u8> = (0..=u8::MAX).collect();
        assert_eq!(decode_base64(&base64(&every)), Ok(every));
    }

    #[test]
    fn malformed_base64_is_reported_rather_than_guessed() {
        assert!(decode_base64("Zm9").is_err(), "a short group is refused");
        assert!(
            decode_base64("Zm9vY").is_err(),
            "a trailing group is refused"
        );
        assert!(
            decode_base64("Z!9v").is_err(),
            "a stray character is refused"
        );
        assert!(decode_base64("=m9v").is_err(), "leading padding is refused");
        assert!(decode_base64("Zm=v").is_err(), "inner padding is refused");
        assert!(decode_base64("Zm9vYg==Zm9v").is_err(), "padding must trail");
    }
}
