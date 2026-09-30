//! JNI entry points.
//!
//! This file owns the exported symbol names only, because the Kotlin `external fun` declarations
//! inside `org.kotlogramme.TelegramClient.Native` bind to them. Everything else lives in the
//! modules below.

mod client;
mod dto;
mod error;
mod ops;
mod payload;

use std::path::PathBuf;

use grammers_session::Session;
use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jbyteArray, jlong};
use jni::JNIEnv;

use crate::client::{close_client, create_client, get_client, resolve_peer};
use crate::dto::message_dto;
use crate::dto::user_dto;
use crate::error::{error, invocation_error, java_bytes, java_string, json_string, read_string};
use crate::ops::dispatch;
use crate::payload::PeerTarget;

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_create(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    api_id: i32,
    api_hash: JString<'_>,
    session_path: JString<'_>,
) -> jni::sys::jstring {
    let result = (|| {
        let api_hash = read_string(&mut env, api_hash)?;
        let path = PathBuf::from(read_string(&mut env, session_path)?);
        create_client(api_id, api_hash, path)
    })();

    java_string(&mut env, result.unwrap_or_else(error))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_close(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
) -> jni::sys::jstring {
    java_string(&mut env, close_client(handle))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_invokeRaw(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
    body: JByteArray<'_>,
    data_center_id: i32,
) -> jbyteArray {
    let result = (|| {
        let body = env.convert_byte_array(body).map_err(error)?;
        let native = get_client(handle)?;
        let data_center_id = if data_center_id > 0 {
            data_center_id
        } else {
            native.session.home_dc_id().map_err(error)?
        };
        native
            .runtime
            .block_on(native.sender.raw_invoke_in_dc(data_center_id, body))
            .map_err(invocation_error)
    })();
    java_bytes(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_isAuthorized(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
) -> jni::sys::jstring {
    let result = get_client(handle).and_then(|native| {
        native
            .runtime
            .block_on(native.client.is_authorized())
            .map(|authorized| authorized.to_string())
            .map_err(invocation_error)
    });
    java_string(&mut env, result.unwrap_or_else(error))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_signInBot(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
    token: JString<'_>,
    api_hash: JString<'_>,
) -> jni::sys::jstring {
    let result = (|| {
        let token = read_string(&mut env, token)?;
        let api_hash = read_string(&mut env, api_hash)?;
        let native = get_client(handle)?;
        let user = native
            .runtime
            .block_on(native.client.bot_sign_in(&token, &api_hash))
            .map_err(invocation_error)?;
        json_string(user_dto(&user))
    })();
    java_string(&mut env, result.unwrap_or_else(error))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_sendMessage(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
    username: JString<'_>,
    text: JString<'_>,
) -> jni::sys::jstring {
    let result = (|| {
        let username = read_string(&mut env, username)?;
        let text = read_string(&mut env, text)?;
        let native = get_client(handle)?;
        let peer = native.runtime.block_on(resolve_peer(
            &native,
            &PeerTarget {
                peer_handle: None,
                username: Some(username),
            },
        ))?;
        let message = native
            .runtime
            .block_on(native.client.send_message(peer, text))
            .map_err(invocation_error)?;
        json_string(message_dto(&message))
    })();
    java_string(&mut env, result.unwrap_or_else(error))
}

#[no_mangle]
pub extern "system" fn Java_org_kotlogramme_TelegramClient_00024Native_request(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
    operation: JString<'_>,
    payload: JString<'_>,
) -> jni::sys::jstring {
    let result = (|| {
        let operation = read_string(&mut env, operation)?;
        let payload = read_string(&mut env, payload)?;
        let native = get_client(handle)?;
        dispatch(&native, &operation, &payload)
    })();
    java_string(&mut env, result.unwrap_or_else(error))
}
