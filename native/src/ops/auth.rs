//! Authentication and account operations.

use grammers_client::SignInError;
use grammers_session::Session;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer_projected, AuthenticationState, NativeClient};
use crate::dto::auth::{data_centre_dto, me_dto};
use crate::dto::peer::peer_dto;
use crate::dto::user_dto;
use crate::error::{error, invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

#[derive(Deserialize)]
struct PhonePayload {
    phone: String,
}

#[derive(Deserialize)]
struct CodePayload {
    code: String,
}

#[derive(Deserialize)]
struct PasswordPayload {
    password: String,
}

#[cfg_attr(not(test), allow(dead_code))]
/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "requestLoginCode",
    "signIn",
    "checkPassword",
    "signOut",
    "getMe",
    "getDataCentreId",
    "resolveUsername",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
///
/// The unit tests in [`super::tests`](crate::ops::tests) assert that every name in
/// [`OPERATIONS`] routes here, so an operation is added by naming it in both places.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "requestLoginCode" => request_login_code,
        "signIn" => sign_in,
        "checkPassword" => check_password,
        "signOut" => sign_out,
        "getMe" => get_me,
        "getDataCentreId" => get_data_centre_id,
        "resolveUsername" => resolve_username,
        _ => return None,
    })
}

fn request_login_code(native: &NativeClient, payload: &str) -> Result<String, String> {
    let PhonePayload { phone } = parse_payload(payload)?;
    let token = native
        .runtime
        .block_on(native.client.request_login_code(&phone, &native.api_hash))
        .map_err(invocation_error)?;
    *native
        .authentication
        .lock()
        .map_err(|_| "authentication state is poisoned".to_owned())? =
        AuthenticationState::LoginCode(token);
    json_string(json!({ "phone": phone }))
}

fn sign_in(native: &NativeClient, payload: &str) -> Result<String, String> {
    let CodePayload { code } = parse_payload(payload)?;
    let token = match std::mem::replace(
        &mut *native
            .authentication
            .lock()
            .map_err(|_| "authentication state is poisoned".to_owned())?,
        AuthenticationState::Idle,
    ) {
        AuthenticationState::LoginCode(token) => token,
        _ => return Err("requestLoginCode must be called before signIn".to_owned()),
    };

    match native
        .runtime
        .block_on(native.client.sign_in(&token, &code))
    {
        Ok(user) => json_string(json!({ "status": "authorized", "user": user_dto(&user) })),
        Err(SignInError::PasswordRequired(password_token)) => {
            let hint = password_token.hint().map(ToOwned::to_owned);
            *native
                .authentication
                .lock()
                .map_err(|_| "authentication state is poisoned".to_owned())? =
                AuthenticationState::Password(password_token);
            json_string(json!({ "status": "passwordRequired", "hint": hint }))
        }
        Err(SignInError::InvalidCode) => {
            *native
                .authentication
                .lock()
                .map_err(|_| "authentication state is poisoned".to_owned())? =
                AuthenticationState::LoginCode(token);
            Err("invalid login code".to_owned())
        }
        Err(error) => Err(error.to_string()),
    }
}

fn check_password(native: &NativeClient, payload: &str) -> Result<String, String> {
    let PasswordPayload { password } = parse_payload(payload)?;
    let token = match std::mem::replace(
        &mut *native
            .authentication
            .lock()
            .map_err(|_| "authentication state is poisoned".to_owned())?,
        AuthenticationState::Idle,
    ) {
        AuthenticationState::Password(token) => token,
        _ => return Err("signIn must report passwordRequired before checkPassword".to_owned()),
    };
    let user = native
        .runtime
        .block_on(native.client.check_password(token, password.as_bytes()))
        .map_err(|error| error.to_string())?;
    json_string(user_dto(&user))
}

/// Signs the session out of the account it is authorized with.
///
/// grammers keeps the client connected after signing out; only the authorization is dropped, so a
/// later call has to sign in again. The in-progress login state is cleared as well, because a code
/// or password token from before the sign-out is no longer valid.
fn sign_out(native: &NativeClient, _payload: &str) -> Result<String, String> {
    native
        .runtime
        .block_on(native.client.sign_out())
        .map_err(invocation_error)?;
    *native
        .authentication
        .lock()
        .map_err(|_| "authentication state is poisoned".to_owned())? = AuthenticationState::Idle;
    json_string(json!({ "ok": true }))
}

/// Fetches the account associated with the current session, made full by the data centre it lives
/// on.
///
/// grammers reports the two separately: the account comes from `users.getUsers[UserSelf]` and the
/// data centre from the session, which is why both are projected together here.
fn get_me(native: &NativeClient, _payload: &str) -> Result<String, String> {
    let user = native
        .runtime
        .block_on(native.client.get_me())
        .map_err(invocation_error)?;
    let data_centre_id = native.session.home_dc_id().map_err(error)?;
    json_string(me_dto(&user, data_centre_id))
}

/// Reports the home data centre of the session, which is what a raw call defaults to.
fn get_data_centre_id(native: &NativeClient, _payload: &str) -> Result<String, String> {
    json_string(data_centre_dto(native.session.home_dc_id().map_err(error)?))
}

fn resolve_username(native: &NativeClient, payload: &str) -> Result<String, String> {
    let target: PeerTarget = parse_payload(payload)?;
    let peer = native
        .runtime
        .block_on(resolve_peer_projected(native, &target))?;
    json_string(peer_dto(native, &peer)?)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::ptr::fn_addr_eq;

    #[test]
    fn the_auth_operations_route_to_their_own_handler() {
        assert!(fn_addr_eq(
            route("signOut").expect("routed"),
            sign_out as Handler,
        ));
        assert!(fn_addr_eq(
            route("getMe").expect("routed"),
            get_me as Handler,
        ));
        assert!(fn_addr_eq(
            route("getDataCentreId").expect("routed"),
            get_data_centre_id as Handler,
        ));
    }

    #[test]
    fn a_foreign_operation_is_not_routed_by_this_module() {
        assert!(route("sendMessage").is_none());
        assert!(route("getDataCentre").is_none());
    }
}
