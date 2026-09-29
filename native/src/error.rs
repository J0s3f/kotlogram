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

/// Reads a Java string argument.
pub(crate) fn read_string(env: &mut JNIEnv<'_>, value: JString<'_>) -> Result<String, String> {
    env.get_string(&value)
        .map(|value| value.to_string_lossy().into_owned())
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
