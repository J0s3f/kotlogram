//! Error and JNI marshalling helpers shared by the client registry, the operation modules and the
//! exported JNI functions.

use grammers_client::InvocationError;
use jni::objects::JString;
use jni::sys::{jbyteArray, jstring};
use jni::JNIEnv;
use serde::de::DeserializeOwned;
use serde::Serialize;

/// Prefixes every message that crosses the JNI boundary as an error string.
pub(crate) fn error(value: impl std::fmt::Display) -> String {
    format!("kotlogramme error: {value}")
}

/// Decodes the bytes the JNI `GetStringUTFChars` encoder produced.
///
/// JNI speaks Modified UTF-8 (CESU-8), not standard UTF-8. A supplementary code point - most
/// emoji, every astral-plane character - is spelled as a surrogate pair there, two three-byte
/// sequences that are invalid as standard UTF-8, and the null character is spelled `C0 80` so a
/// C string can carry it. Decoding those bytes with `CStr::to_string_lossy` - which is what a
/// `JavaStr` reaches through its `JNIStr` -> `CStr` deref - turned one emoji into six
/// replacement characters, which is exactly the corruption a live check measured. The jni crate
/// keeps the correct decoder underneath (`From<&JNIStr> for Cow<str>`); this calls it directly
/// so the behaviour is a testable function instead of a deref accident.
pub(crate) fn decode_java_string(bytes: &[u8]) -> String {
    match cesu8::from_java_cesu8(bytes) {
        Ok(value) => value.into_owned(),
        // Unreachable for anything `GetStringUTFChars` can produce, but an everyday lossy decode
        // keeps the failure non-panicking if a future JNI encoder ever disagrees.
        Err(_) => String::from_utf8_lossy(bytes).into_owned(),
    }
}

/// Reads a Java string argument.
pub(crate) fn read_string(env: &mut JNIEnv<'_>, value: JString<'_>) -> Result<String, String> {
    env.get_string(&value)
        .map(|value| decode_java_string(value.to_bytes()))
        .map_err(|error| format!("invalid Java string: {error}"))
}

/// Returns a Java string, or throws [crate::TelegramException] when [value] is an error message.
pub(crate) fn java_string(env: &mut JNIEnv<'_>, value: String) -> jstring {
    env.new_string(value)
        .map_or(std::ptr::null_mut(), |value| value.into_raw())
}

/// Returns a Java byte array, or throws [crate::TelegramException] when [value] is an error
/// message.
pub(crate) fn java_bytes(env: &mut JNIEnv<'_>, value: Result<Vec<u8>, String>) -> jbyteArray {
    match value {
        Ok(value) => env
            .byte_array_from_slice(&value)
            .map_or(std::ptr::null_mut(), |value| value.into_raw()),
        Err(message) => {
            let _ = env.throw_new("org/kotlogramme/TelegramException", error(message));
            std::ptr::null_mut()
        }
    }
}

/// Decodes a JSON request payload.
pub(crate) fn parse_payload<T: DeserializeOwned>(payload: &str) -> Result<T, String> {
    serde_json::from_str(payload).map_err(|error| format!("invalid request payload: {error}"))
}

/// Encodes a JSON operation result.
pub(crate) fn json_string(value: impl Serialize) -> Result<String, String> {
    serde_json::to_string(&value).map_err(error)
}

/// Renders a grammers invocation failure.
pub(crate) fn invocation_error(error: InvocationError) -> String {
    error.to_string()
}

#[cfg(test)]
mod tests {
    use super::decode_java_string;

    #[test]
    fn bmp_characters_pass_through_unchanged() {
        // For BMP characters CESU-8 is byte-identical to standard UTF-8: "Пр ok".
        let bytes = [0xD0, 0x9F, 0xD1, 0x80, b' ', b'o', b'k'];
        assert_eq!(decode_java_string(&bytes), "\u{41F}\u{440} ok");
    }

    #[test]
    fn supplementary_code_points_reassemble_from_surrogate_pairs() {
        // U+1F600 as GetStringUTFChars hands it over: two three-byte CESU-8 surrogate
        // sequences. The plain-lossy decode this replaces produced six replacement
        // characters, which is the corruption a live send measured end to end.
        let bytes = [0xED, 0xA0, 0xBD, 0xED, 0xB8, 0x80];
        let decoded = decode_java_string(&bytes);
        assert_eq!(decoded, "\u{1F600}");
        assert_eq!(decoded.as_bytes(), &[0xF0, 0x9F, 0x98, 0x80]);
    }

    #[test]
    fn the_java_encoded_nul_round_trips() {
        // Modified UTF-8 spells the null character C0 80 so a C string can carry it; the
        // deref-accident decode turned those two bytes into replacement characters too.
        let bytes = [b'a', 0xC0, 0x80, b'b'];
        assert_eq!(decode_java_string(&bytes), "a\u{0}b");
    }
}
