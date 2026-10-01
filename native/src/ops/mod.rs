//! Operation dispatch.
//!
//! Every domain owns one module below. A module declares the operation names it answers for in
//! [`OPERATIONS`] and implements [`route`], so adding a domain is one `mod` line here plus the
//! module's own file.

pub(crate) mod account;
pub(crate) mod actions;
pub(crate) mod auth;
pub(crate) mod chatlists;
pub(crate) mod chats;
pub(crate) mod contacts;
pub(crate) mod dialogs;
pub(crate) mod files;
pub(crate) mod folders;
pub(crate) mod inline;
pub(crate) mod markup;
pub(crate) mod media;
pub(crate) mod messages;
pub(crate) mod raw;
pub(crate) mod stickers;
pub(crate) mod updates;
pub(crate) mod users;

#[cfg(test)]
mod tests;

use crate::client::NativeClient;

/// Signature shared by every operation handler: it receives the JSON payload and returns the JSON
/// result, or the message Kotlin turns into a `TelegramException`.
pub(crate) type Handler = fn(&NativeClient, &str) -> Result<String, String>;

/// Every module's `route`, in dispatch order.
const ROUTES: &[fn(&str) -> Option<Handler>] = &[
    account::route,
    auth::route,
    chatlists::route,
    messages::route,
    chats::route,
    contacts::route,
    dialogs::route,
    updates::route,
    media::route,
    stickers::route,
    files::route,
    folders::route,
    inline::route,
    actions::route,
    markup::route,
    raw::route,
    users::route,
];

#[cfg_attr(not(test), allow(dead_code))]
/// Every module's [`OPERATIONS`], in the same order as [`ROUTES`].
const INVENTORY: &[&[&str]] = &[
    account::OPERATIONS,
    auth::OPERATIONS,
    chatlists::OPERATIONS,
    messages::OPERATIONS,
    chats::OPERATIONS,
    contacts::OPERATIONS,
    dialogs::OPERATIONS,
    updates::OPERATIONS,
    media::OPERATIONS,
    stickers::OPERATIONS,
    files::OPERATIONS,
    folders::OPERATIONS,
    inline::OPERATIONS,
    actions::OPERATIONS,
    markup::OPERATIONS,
    raw::OPERATIONS,
    users::OPERATIONS,
];

/// Runs one operation by name.
pub(crate) fn dispatch(
    native: &NativeClient,
    operation: &str,
    payload: &str,
) -> Result<String, String> {
    if let Some(handler) = route(operation) {
        return handler(native, payload);
    }
    Err(format!("unsupported operation: {operation}"))
}

/// Finds the module that owns [operation], if any.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    ROUTES.iter().find_map(|route| route(operation))
}

#[cfg_attr(not(test), allow(dead_code))]
/// Every operation name routed through [`dispatch`].
pub(crate) fn all_operations() -> Vec<&'static str> {
    INVENTORY
        .iter()
        .flat_map(|operations| operations.iter().copied())
        .collect()
}
