//! User lookup by numeric id.
//!
//! grammers 0.8.1 has no typed "fetch users by id" helper: `Client::get_me` and
//! `Client::resolve_peer` both build the layer's `users.getUsers` request themselves, one id at a
//! time. This family does the same for a batch of bare ids, which is the shape a caller has after
//! reading ids off a page of messages. Every answer reuses the shared contacts projection, so a
//! resolved account arrives with the same fields and the same registered peer handle the contacts
//! family returns.

use std::collections::HashSet;

use grammers_client::tl;
use grammers_session::types::PeerId;
use grammers_session::Session;
use serde::Deserialize;

use super::Handler;
use crate::client::NativeClient;
use crate::dto::contacts::{contact_user_dto, ContactUserDto};
use crate::error::{error, invocation_error, json_string, parse_payload};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct UsersPayload {
    ids: Vec<i64>,
}

#[cfg_attr(not(test), allow(dead_code))]
/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &["getUsers"];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "getUsers" => get_users,
        _ => return None,
    })
}

/// Resolves each requested id to the account it names, which is the layer's `users.getUsers`.
///
/// The request carries one `inputUser` per distinct id, using the access hash the session already
/// cached for it and the ambient one (zero) otherwise; Telegram answers the ids it can resolve and
/// omits or empties the rest, which is why the projection drops a `userEmpty` entry.
fn get_users(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: UsersPayload = parse_payload(payload)?;
    let ids = unique_ids(&data.ids);
    if ids.is_empty() {
        return json_string(Vec::<ContactUserDto>::new());
    }
    let users = native.runtime.block_on(async {
        let mut inputs = Vec::with_capacity(ids.len());
        for id in &ids {
            let peer_id = PeerId::user(*id).ok_or_else(|| format!("not a user id: {id}"))?;
            let auth = native
                .session
                .peer_ref(peer_id)
                .await
                .map_err(error)?
                .map(|peer| peer.auth)
                .unwrap_or_default();
            inputs.push(tl::enums::InputUser::User(tl::types::InputUser {
                user_id: *id,
                access_hash: auth.hash(),
            }));
        }
        native
            .client
            .invoke(&tl::functions::users::GetUsers { id: inputs })
            .await
            .map_err(invocation_error)
    })?;
    let mut projected = Vec::with_capacity(users.len());
    for user in &users {
        if is_resolved(user) {
            projected.push(contact_user_dto(native, user)?);
        }
    }
    json_string(projected)
}

/// Deduplicates the requested ids, keeping the first time each one is seen.
fn unique_ids(ids: &[i64]) -> Vec<i64> {
    let mut seen = HashSet::new();
    ids.iter().copied().filter(|id| seen.insert(*id)).collect()
}

/// Whether the layer answered a real account for an id. An id Telegram cannot resolve comes back
/// as `userEmpty`, which carries nothing to project.
fn is_resolved(user: &tl::enums::User) -> bool {
    matches!(user, tl::enums::User::User(_))
}

#[cfg(test)]
mod tests {
    //! Payload-decode and pure-helper tests.
    //!
    //! The handler itself needs a live Telegram session, so what is pinned here is the wire shape
    //! Kotlin sends, the id deduplication and the empty-answer filter.

    use super::*;

    fn decode<T: for<'de> Deserialize<'de>>(json: &str) -> T {
        serde_json::from_str(json).expect("the payload decodes")
    }

    #[test]
    fn a_users_payload_reads_the_id_list() {
        let data: UsersPayload = decode(r#"{"ids": [7, 8, 9]}"#);
        assert_eq!(data.ids, vec![7, 8, 9]);
    }

    #[test]
    fn an_empty_id_list_decodes_to_an_empty_request() {
        let data: UsersPayload = decode(r#"{"ids": []}"#);
        assert!(data.ids.is_empty());
        assert!(unique_ids(&data.ids).is_empty());
    }

    #[test]
    fn duplicate_ids_are_asked_for_once_in_first_seen_order() {
        assert_eq!(unique_ids(&[9, 7, 9, 8, 7]), vec![9, 7, 8]);
        assert_eq!(unique_ids(&[7]), vec![7]);
    }

    #[test]
    fn an_id_telegram_does_not_resolve_is_dropped() {
        let empty = tl::enums::User::Empty(tl::types::UserEmpty { id: 7 });
        assert!(!is_resolved(&empty));
    }

    #[test]
    fn the_get_users_operation_routes_to_its_handler() {
        assert_eq!(route("getUsers"), Some(get_users as Handler));
    }

    #[test]
    fn a_foreign_operation_is_not_routed_by_this_module() {
        assert!(route("getMe").is_none());
        assert!(route("contactsGetContacts").is_none());
    }
}
