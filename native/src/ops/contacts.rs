//! Typed contacts operations.
//!
//! grammers exposes no high-level contacts surface, so this family is built on its TL layer the
//! same way `acceptInviteLink` is: build a `tl::functions::contacts::*` request, invoke it through
//! the client and project the result. Every peer-bearing answer reuses the shared projections, so
//! the peers it returns are registered and sendable back to the native side.

use grammers_client::tl;
use serde::Deserialize;
use serde_json::json;

use super::Handler;
use crate::client::{resolve_peer, NativeClient};
use crate::dto::contacts::{
    contact_peer_dto, contact_users_dto, millis, BlockedContactsDto, BlockedPeerDto, ContactDto,
    ContactPeerDto, ContactsDto, FoundContactsDto, ImportedContactDto, ImportedContactsDto,
    PopularInviteDto,
};
use crate::dto::peer::PeerDto;
use crate::dto::user::UserDto;
use crate::error::{invocation_error, json_string, parse_payload};
use crate::payload::PeerTarget;

/// The layer caps both `contacts.getBlocked` and `contacts.search` at one page of 100.
const MAX_CONTACTS_LIMIT: i32 = 100;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GetContactsPayload {
    /// The saved-contacts hash a previous answer reported; absent asks for the full list.
    #[serde(default)]
    hash: i64,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ImportContactsPayload {
    contacts: Vec<ImportContactPayload>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ImportContactPayload {
    client_id: i64,
    phone: String,
    first_name: String,
    #[serde(default)]
    last_name: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct DeleteContactsPayload {
    users: Vec<PeerTarget>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct BlockContactPayload {
    #[serde(flatten)]
    peer: PeerTarget,
    #[serde(default)]
    my_stories_from: bool,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct GetBlockedPayload {
    #[serde(default)]
    my_stories_from: bool,
    #[serde(default)]
    offset: i32,
    limit: Option<i32>,
    /// Return every blocked peer by walking the layer's pages in one call. Wins over `offset` and
    /// `limit`.
    #[serde(default)]
    all: bool,
}

/// What a get-blocked request asks for: every blocked peer, or one page.
enum BlockedPlan {
    /// `all: true` — walk the layer's pages from the top until one comes back short.
    All,
    /// One page: the given offset and the clamped limit.
    Page { offset: i32, limit: i32 },
}

/// Resolves a get-blocked payload into the plan the handler follows.
///
/// `all` wins over both `offset` and `limit`: it is the explicit request for the whole list, so an
/// offset or limit sent with it is a caller error that `all` overrides rather than honours.
fn blocked_plan(data: &GetBlockedPayload) -> BlockedPlan {
    if data.all {
        BlockedPlan::All
    } else {
        BlockedPlan::Page {
            offset: data.offset,
            limit: data
                .limit
                .unwrap_or(MAX_CONTACTS_LIMIT)
                .clamp(1, MAX_CONTACTS_LIMIT),
        }
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct SearchContactsPayload {
    q: String,
    #[serde(default)]
    broadcasts: bool,
    #[serde(default)]
    bots: bool,
    limit: Option<i32>,
}

#[cfg_attr(not(test), allow(dead_code))]
/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[
    "contactsGetContacts",
    "contactsImportContacts",
    "contactsDeleteContacts",
    "contactsBlock",
    "contactsUnblock",
    "contactsGetBlocked",
    "contactsSearch",
];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
pub(crate) fn route(operation: &str) -> Option<Handler> {
    Some(match operation {
        "contactsGetContacts" => get_contacts,
        "contactsImportContacts" => import_contacts,
        "contactsDeleteContacts" => delete_contacts,
        "contactsBlock" => block,
        "contactsUnblock" => unblock,
        "contactsGetBlocked" => get_blocked,
        "contactsSearch" => search,
        _ => return None,
    })
}

/// Lists the account's saved contacts, answering the layer's `contactsNotModified` as an empty
/// page.
fn get_contacts(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetContactsPayload = parse_payload(payload)?;
    let result = native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::contacts::GetContacts { hash: data.hash }),
        )
        .map_err(invocation_error)?;
    let dto = match result {
        tl::enums::contacts::Contacts::NotModified => ContactsDto {
            not_modified: true,
            saved_count: 0,
            contacts: Vec::new(),
            users: Vec::new(),
        },
        tl::enums::contacts::Contacts::Contacts(contacts) => contacts_dto(native, contacts)?,
    };
    json_string(dto)
}

/// Projects `contacts.contacts`, resolving each entry's named user against the answer's users.
fn contacts_dto(
    native: &NativeClient,
    contacts: tl::types::contacts::Contacts,
) -> Result<ContactsDto, String> {
    let mut rows = Vec::with_capacity(contacts.contacts.len());
    for contact in &contacts.contacts {
        let tl::enums::Contact::Contact(contact) = contact;
        let reference = tl::enums::Peer::User(tl::types::PeerUser {
            user_id: contact.user_id,
        });
        let (user, peer) = split(contact_peer_dto(native, &reference, &contacts.users, &[])?);
        rows.push(ContactDto {
            user_id: contact.user_id,
            mutual: contact.mutual,
            user,
            peer,
        });
    }
    Ok(ContactsDto {
        not_modified: false,
        saved_count: contacts.saved_count,
        contacts: rows,
        users: contact_users_dto(native, &contacts.users)?,
    })
}

/// Imports phone contacts, projecting the imported, retried and popular entries.
fn import_contacts(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: ImportContactsPayload = parse_payload(payload)?;
    let request = tl::functions::contacts::ImportContacts {
        contacts: data
            .contacts
            .iter()
            .map(|contact| {
                tl::enums::InputContact::InputPhoneContact(tl::types::InputPhoneContact {
                    client_id: contact.client_id,
                    phone: contact.phone.clone(),
                    first_name: contact.first_name.clone(),
                    last_name: contact.last_name.clone(),
                    note: None,
                })
            })
            .collect(),
    };
    let result = native
        .runtime
        .block_on(native.client.invoke(&request))
        .map_err(invocation_error)?;
    let tl::enums::contacts::ImportedContacts::Contacts(imported) = result;
    let mut rows = Vec::with_capacity(imported.imported.len());
    for entry in &imported.imported {
        let tl::enums::ImportedContact::Contact(entry) = entry;
        let reference = tl::enums::Peer::User(tl::types::PeerUser {
            user_id: entry.user_id,
        });
        let (user, peer) = split(contact_peer_dto(native, &reference, &imported.users, &[])?);
        rows.push(ImportedContactDto {
            user_id: entry.user_id,
            client_id: entry.client_id,
            user,
            peer,
        });
    }
    json_string(ImportedContactsDto {
        imported: rows,
        retry_contacts: imported.retry_contacts.clone(),
        popular_invites: imported
            .popular_invites
            .iter()
            .map(|invite| {
                let tl::enums::PopularContact::Contact(invite) = invite;
                PopularInviteDto {
                    client_id: invite.client_id,
                    importers: invite.importers,
                }
            })
            .collect(),
        users: contact_users_dto(native, &imported.users)?,
    })
}

/// Deletes saved contacts by resolving each peer and sending the layer's `deleteContacts`.
fn delete_contacts(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: DeleteContactsPayload = parse_payload(payload)?;
    let mut ids = Vec::with_capacity(data.users.len());
    for target in &data.users {
        let peer = native.runtime.block_on(resolve_peer(native, target))?;
        ids.push(tl::enums::InputUser::from(peer));
    }
    native
        .runtime
        .block_on(
            native
                .client
                .invoke(&tl::functions::contacts::DeleteContacts { id: ids }),
        )
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Blocks a peer, which is the layer's `contacts.block`.
fn block(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: BlockContactPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    native
        .runtime
        .block_on(native.client.invoke(&tl::functions::contacts::Block {
            my_stories_from: data.my_stories_from,
            id: tl::enums::InputPeer::from(peer),
        }))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Unblocks a peer, which is the layer's `contacts.unblock`.
fn unblock(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: BlockContactPayload = parse_payload(payload)?;
    let peer = native.runtime.block_on(resolve_peer(native, &data.peer))?;
    native
        .runtime
        .block_on(native.client.invoke(&tl::functions::contacts::Unblock {
            my_stories_from: data.my_stories_from,
            id: tl::enums::InputPeer::from(peer),
        }))
        .map_err(invocation_error)?;
    json_string(json!({ "ok": true }))
}

/// Lists the account's blocked peers, which is the layer's `contacts.getBlocked`.
///
/// Cost note: unlike the iterator-based listings, this pages through the layer's raw
/// `contacts.GetBlocked` request, which takes an explicit `offset` — so a page is O(1) and paging
/// deep is cheap (no re-walk). `all: true` walks those pages in one call — O(n) pages of
/// [`MAX_CONTACTS_LIMIT`] — which is how the CLI's `--all` is served.
fn get_blocked(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: GetBlockedPayload = parse_payload(payload)?;
    match blocked_plan(&data) {
        BlockedPlan::All => get_all_blocked(native, data.my_stories_from),
        BlockedPlan::Page { offset, limit } => {
            get_blocked_page(native, data.my_stories_from, offset, limit)
        }
    }
}

/// Fetches one page of blocked peers and projects it.
fn get_blocked_page(
    native: &NativeClient,
    my_stories_from: bool,
    offset: i32,
    limit: i32,
) -> Result<String, String> {
    let result = native
        .runtime
        .block_on(native.client.invoke(&tl::functions::contacts::GetBlocked {
            my_stories_from,
            offset,
            limit,
        }))
        .map_err(invocation_error)?;
    // The layer answers either the full list or a slice that also carries the total count.
    let (count, blocked, chats, users) = match result {
        tl::enums::contacts::Blocked::Blocked(blocked) => {
            (None, blocked.blocked, blocked.chats, blocked.users)
        }
        tl::enums::contacts::Blocked::Slice(slice) => {
            (Some(slice.count), slice.blocked, slice.chats, slice.users)
        }
    };
    project_blocked(native, blocked, chats, users, count)
}

/// Walks every page of blocked peers in one call and projects the whole list.
///
/// The layer caps `contacts.getBlocked` at [`MAX_CONTACTS_LIMIT`] per request, so the whole list
/// is fetched a page at a time, advancing the offset by the number of peers each page returned,
/// until a page comes back short. Each page is O(1) (the request takes an explicit offset), so the
/// walk is O(n) overall.
fn get_all_blocked(native: &NativeClient, my_stories_from: bool) -> Result<String, String> {
    let mut blocked = Vec::new();
    let mut chats = Vec::new();
    let mut users = Vec::new();
    let mut offset = 0;
    loop {
        let result = native
            .runtime
            .block_on(native.client.invoke(&tl::functions::contacts::GetBlocked {
                my_stories_from,
                offset,
                limit: MAX_CONTACTS_LIMIT,
            }))
            .map_err(invocation_error)?;
        let (page_blocked, page_chats, page_users) = match result {
            tl::enums::contacts::Blocked::Blocked(page) => (page.blocked, page.chats, page.users),
            tl::enums::contacts::Blocked::Slice(page) => (page.blocked, page.chats, page.users),
        };
        let page_len = page_blocked.len();
        blocked.extend(page_blocked);
        chats.extend(page_chats);
        users.extend(page_users);
        // A short page (or the full-list answer) means the walk is done.
        if page_len < MAX_CONTACTS_LIMIT as usize {
            break;
        }
        offset += page_len as i32;
    }
    // The whole list was walked, so there is no separate total to report.
    project_blocked(native, blocked, chats, users, None)
}

/// Projects the blocked peers the layer returned.
fn project_blocked(
    native: &NativeClient,
    blocked: Vec<tl::enums::PeerBlocked>,
    chats: Vec<tl::enums::Chat>,
    users: Vec<tl::enums::User>,
    count: Option<i32>,
) -> Result<String, String> {
    let mut rows = Vec::with_capacity(blocked.len());
    for entry in &blocked {
        let tl::enums::PeerBlocked::Blocked(entry) = entry;
        if let Some(projected) = contact_peer_dto(native, &entry.peer_id, &users, &chats)? {
            rows.push(BlockedPeerDto {
                date: millis(entry.date),
                user: projected.user,
                peer: projected.peer,
            });
        }
    }
    json_string(BlockedContactsDto {
        count,
        blocked: rows,
        users: contact_users_dto(native, &users)?,
    })
}

/// Searches contacts and public peers, which is the layer's `contacts.search`.
fn search(native: &NativeClient, payload: &str) -> Result<String, String> {
    let data: SearchContactsPayload = parse_payload(payload)?;
    let limit = data.limit.unwrap_or(50).clamp(1, MAX_CONTACTS_LIMIT);
    let result = native
        .runtime
        .block_on(native.client.invoke(&tl::functions::contacts::Search {
            broadcasts: data.broadcasts,
            bots: data.bots,
            q: data.q.clone(),
            limit,
        }))
        .map_err(invocation_error)?;
    let found = match result {
        tl::enums::contacts::Found::Found(found) => found,
    };
    json_string(FoundContactsDto {
        my_results: project_peers(native, &found.my_results, &found.users, &found.chats)?,
        results: project_peers(native, &found.results, &found.users, &found.chats)?,
        users: contact_users_dto(native, &found.users)?,
    })
}

/// Resolves each layer peer reference against the answer's own users and chats, skipping a peer
/// the answer did not describe.
fn project_peers(
    native: &NativeClient,
    peers: &[tl::enums::Peer],
    users: &[tl::enums::User],
    chats: &[tl::enums::Chat],
) -> Result<Vec<ContactPeerDto>, String> {
    let mut rows = Vec::with_capacity(peers.len());
    for peer in peers {
        if let Some(projected) = contact_peer_dto(native, peer, users, chats)? {
            rows.push(projected);
        }
    }
    Ok(rows)
}

/// Splits a projected peer into the optional account and the optional handle a saved-contact row
/// carries.
fn split(projected: Option<ContactPeerDto>) -> (Option<UserDto>, Option<PeerDto>) {
    match projected {
        Some(entry) => (entry.user, Some(entry.peer)),
        None => (None, None),
    }
}

#[cfg(test)]
mod tests {
    //! Payload-decode tests.
    //!
    //! The handlers themselves need a live Telegram session, so what is pinned here is the wire
    //! shape Kotlin sends: the camelCase names, the optional fields and the flattened peer target.

    use super::*;

    fn decode<T: for<'de> Deserialize<'de>>(json: &str) -> T {
        serde_json::from_str(json).expect("the payload decodes")
    }

    #[test]
    fn an_empty_get_contacts_payload_asks_for_the_full_list() {
        let data: GetContactsPayload = decode("{}");
        assert_eq!(data.hash, 0);
        assert_eq!(decode::<GetContactsPayload>(r#"{"hash": 42}"#).hash, 42);
    }

    #[test]
    fn an_import_payload_reads_every_contact() {
        let data: ImportContactsPayload = decode(
            r#"{"contacts": [
                {"clientId": 42, "phone": "+10000000000", "firstName": "Some", "lastName": "One"},
                {"clientId": 43, "phone": "+10000000001", "firstName": "Only"}
            ]}"#,
        );
        assert_eq!(data.contacts.len(), 2);
        assert_eq!(data.contacts[0].client_id, 42);
        assert_eq!(data.contacts[0].phone, "+10000000000");
        assert_eq!(data.contacts[0].first_name, "Some");
        assert_eq!(data.contacts[0].last_name, "One");
        // A missing last name is the empty string, which the layer takes unchanged.
        assert_eq!(data.contacts[1].last_name, "");
    }

    #[test]
    fn a_block_payload_flattens_the_peer_target() {
        let data: BlockContactPayload = decode(r#"{"peerHandle": 7, "myStoriesFrom": true}"#);
        assert_eq!(data.peer.peer_handle, Some(7));
        assert_eq!(data.peer.username, None);
        assert!(data.my_stories_from);

        let data: BlockContactPayload = decode(r#"{"username": "@someone"}"#);
        assert_eq!(data.peer.username.as_deref(), Some("@someone"));
        assert!(!data.my_stories_from);
    }

    #[test]
    fn a_get_blocked_payload_defaults_to_the_first_page() {
        let data: GetBlockedPayload = decode("{}");
        assert!(!data.my_stories_from);
        assert_eq!(data.offset, 0);
        assert_eq!(data.limit, None);
        assert!(!data.all);

        let data: GetBlockedPayload = decode(r#"{"offset": 100, "limit": 25}"#);
        assert_eq!(data.offset, 100);
        assert_eq!(data.limit, Some(25));
        assert!(!data.all);

        let data: GetBlockedPayload = decode(r#"{"all": true}"#);
        assert!(data.all);
    }

    #[test]
    fn blocked_all_wins_over_the_offset_and_the_limit() {
        // `all` with an offset still walks everything from the top: the offset is ignored.
        let data: GetBlockedPayload = decode(r#"{"all": true, "offset": 100, "limit": 25}"#);
        assert!(matches!(blocked_plan(&data), BlockedPlan::All));

        // `all` with a limit and no offset still walks everything.
        let data: GetBlockedPayload = decode(r#"{"all": true, "limit": 25}"#);
        assert!(matches!(blocked_plan(&data), BlockedPlan::All));
    }

    #[test]
    fn a_get_blocked_page_request_keeps_the_old_behaviour() {
        let data: GetBlockedPayload = decode(r#"{"offset": 100, "limit": 25}"#);
        match blocked_plan(&data) {
            BlockedPlan::Page { offset, limit } => {
                assert_eq!(offset, 100);
                assert_eq!(limit, 25);
            }
            BlockedPlan::All => panic!("a page request must not walk everything"),
        }

        // The default page is the first one at the layer's cap.
        let data: GetBlockedPayload = decode("{}");
        match blocked_plan(&data) {
            BlockedPlan::Page { offset, limit } => {
                assert_eq!(offset, 0);
                assert_eq!(limit, MAX_CONTACTS_LIMIT);
            }
            BlockedPlan::All => panic!("a page request must not walk everything"),
        }
    }

    #[test]
    fn a_search_payload_reads_the_query_and_the_flags() {
        let data: SearchContactsPayload = decode(r#"{"q": "ada"}"#);
        assert_eq!(data.q, "ada");
        assert!(!data.broadcasts && !data.bots);
        assert_eq!(data.limit, None);

        let data: SearchContactsPayload =
            decode(r#"{"q": "ada", "broadcasts": true, "bots": true, "limit": 10}"#);
        assert!(data.broadcasts && data.bots);
        assert_eq!(data.limit, Some(10));
    }
}
