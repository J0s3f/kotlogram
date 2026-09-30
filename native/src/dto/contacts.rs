//! Contacts projection.
//!
//! grammers exposes no typed contacts surface, so every answer here is a raw `contacts.*` result.
//! Each peer-bearing projection pairs the rich account description from [`user_dto`] with the
//! registered handle from [`peer_dto`], so a Kotlin caller can send the peer back in a later
//! payload.

use grammers_client::peer::{Peer, User as ClientUser};
use grammers_client::tl;
use grammers_session::types::{PeerId, PeerKind};
use serde::Serialize;

use crate::client::NativeClient;
use crate::dto::peer::{peer_dto, PeerDto};
use crate::dto::user::{user_dto, UserDto};

/// A user together with the registered peer it projects to.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ContactUserDto {
    pub(crate) user: UserDto,
    pub(crate) peer: PeerDto,
}

/// One saved contact: the layer's `Contact`, enriched with the user it names.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ContactDto {
    pub(crate) user_id: i64,
    /// True when Telegram reports the contact as a mutual one.
    pub(crate) mutual: bool,
    /// The account the id names, when the answer carried its user object.
    pub(crate) user: Option<UserDto>,
    pub(crate) peer: Option<PeerDto>,
}

/// The `contactsGetContacts` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ContactsDto {
    /// True for the layer's `contactsNotModified`, which carries no list at all.
    pub(crate) not_modified: bool,
    pub(crate) saved_count: i32,
    pub(crate) contacts: Vec<ContactDto>,
    pub(crate) users: Vec<ContactUserDto>,
}

/// One entry of `contacts.importedContacts.imported`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ImportedContactDto {
    pub(crate) user_id: i64,
    /// The client id the caller assigned to the imported contact.
    pub(crate) client_id: i64,
    /// The account the import produced, when the answer carried its user object.
    pub(crate) user: Option<UserDto>,
    pub(crate) peer: Option<PeerDto>,
}

/// One entry of `contacts.importedContacts.popularInvites`.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PopularInviteDto {
    pub(crate) client_id: i64,
    pub(crate) importers: i32,
}

/// The `contactsImportContacts` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ImportedContactsDto {
    pub(crate) imported: Vec<ImportedContactDto>,
    /// The client ids Telegram could not import; the caller may retry them.
    pub(crate) retry_contacts: Vec<i64>,
    pub(crate) popular_invites: Vec<PopularInviteDto>,
    pub(crate) users: Vec<ContactUserDto>,
}

/// A peer an answer referenced, resolved against the users and chats it carried.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ContactPeerDto {
    /// The account description when the peer is a user; `None` for a chat or channel.
    pub(crate) user: Option<UserDto>,
    pub(crate) peer: PeerDto,
}

/// One blocked peer with the layer's block date.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct BlockedPeerDto {
    /// Epoch milliseconds, from the layer's whole-second date.
    pub(crate) date: i64,
    /// The account description when the blocked peer is a user; `None` for a chat or channel.
    pub(crate) user: Option<UserDto>,
    pub(crate) peer: PeerDto,
}

/// The `contactsGetBlocked` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct BlockedContactsDto {
    /// The total block count, which only the sliced answer carries.
    pub(crate) count: Option<i32>,
    pub(crate) blocked: Vec<BlockedPeerDto>,
    pub(crate) users: Vec<ContactUserDto>,
}

/// The `contactsSearch` document.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct FoundContactsDto {
    /// Peers that matched the query as the viewer's own contacts.
    pub(crate) my_results: Vec<ContactPeerDto>,
    /// Peers that matched the query globally.
    pub(crate) results: Vec<ContactPeerDto>,
    pub(crate) users: Vec<ContactUserDto>,
}

/// Projects one raw user as the pair of its account description and its registered peer.
pub(crate) fn contact_user_dto(
    native: &NativeClient,
    user: &tl::enums::User,
) -> Result<ContactUserDto, String> {
    let client_user = ClientUser::from_raw(&native.client, user.clone());
    let peer = Peer::User(client_user.clone());
    Ok(ContactUserDto {
        user: user_dto(&client_user),
        peer: peer_dto(native, &peer)?,
    })
}

/// Projects a whole answer's user vector.
pub(crate) fn contact_users_dto(
    native: &NativeClient,
    users: &[tl::enums::User],
) -> Result<Vec<ContactUserDto>, String> {
    users
        .iter()
        .map(|user| contact_user_dto(native, user))
        .collect()
}

/// Resolves the layer's peer reference against the users and chats the same answer carried.
///
/// The layer reports a bare `Peer` in `contacts.blocked` and `contacts.found`, while the user and
/// chat objects travel next to it. A peer that names no object in the answer is skipped rather
/// than projected without a handle.
pub(crate) fn contact_peer_dto(
    native: &NativeClient,
    peer: &tl::enums::Peer,
    users: &[tl::enums::User],
    chats: &[tl::enums::Chat],
) -> Result<Option<ContactPeerDto>, String> {
    let id = PeerId::from(peer);
    let Some(bare) = id.bare_id() else {
        return Ok(None);
    };
    match id.kind() {
        PeerKind::User => {
            let Some(user) = users.iter().find(|user| user.id() == bare) else {
                return Ok(None);
            };
            let projected = contact_user_dto(native, user)?;
            Ok(Some(ContactPeerDto {
                user: Some(projected.user),
                peer: projected.peer,
            }))
        }
        PeerKind::Chat | PeerKind::Channel => {
            let Some(chat) = chats.iter().find(|chat| chat.id() == bare) else {
                return Ok(None);
            };
            let projected = Peer::from_raw(&native.client, chat.clone());
            Ok(Some(ContactPeerDto {
                user: None,
                peer: peer_dto(native, &projected)?,
            }))
        }
    }
}

/// The wire format for dates is epoch milliseconds, while the layer reports whole seconds.
pub(crate) fn millis(seconds: i32) -> i64 {
    i64::from(seconds) * 1_000
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the contacts documents.
    //!
    //! Like the other DTO tests, the projection itself needs a live grammers client, so what is
    //! pinned here is the exact document each constructor encodes.

    use super::*;
    use crate::dto::permissions::ChatPermissionsDto;
    use serde::Serialize;

    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    fn user() -> UserDto {
        UserDto {
            id: 7,
            username: Some("someone".to_owned()),
            first_name: Some("Some".to_owned()),
            last_name: Some("One".to_owned()),
            full_name: "Some One".to_owned(),
            usernames: Vec::new(),
            phone: Some("+10000000000".to_owned()),
            photo_id: None,
            status: "offline",
            status_expires: None,
            last_seen: None,
            status_by_me: false,
            lang_code: None,
            is_self: false,
            contact: true,
            mutual_contact: false,
            deleted: false,
            is_bot: false,
            bot_privacy: false,
            bot_supports_chats: false,
            bot_inline_geo: false,
            bot_inline_placeholder: None,
            verified: false,
            restricted: false,
            support: false,
            scam: false,
            restriction_reasons: Vec::new(),
        }
    }

    fn peer() -> PeerDto {
        PeerDto {
            native_handle: 12,
            id: 7,
            kind: "user",
            username: Some("someone".to_owned()),
            name: Some("Some One".to_owned()),
            usernames: Vec::new(),
            is_megagroup: None,
            has_photo: false,
            permissions: None,
        }
    }

    #[test]
    fn a_contacts_document_carries_the_page_and_the_users() {
        let contact = ContactDto {
            user_id: 7,
            mutual: true,
            user: Some(user()),
            peer: Some(peer()),
        };
        assert_json(
            &ContactsDto {
                not_modified: false,
                saved_count: 1,
                contacts: vec![contact],
                users: vec![ContactUserDto {
                    user: user(),
                    peer: peer(),
                }],
            },
            serde_json::json!({
                "notModified": false,
                "savedCount": 1,
                "contacts": [{
                    "userId": 7,
                    "mutual": true,
                    "user": serde_json::to_value(user()).unwrap(),
                    "peer": serde_json::to_value(peer()).unwrap(),
                }],
                "users": [{
                    "user": serde_json::to_value(user()).unwrap(),
                    "peer": serde_json::to_value(peer()).unwrap(),
                }],
            }),
        );
    }

    #[test]
    fn a_not_modified_document_still_carries_the_empty_shape() {
        assert_json(
            &ContactsDto {
                not_modified: true,
                saved_count: 0,
                contacts: Vec::new(),
                users: Vec::new(),
            },
            serde_json::json!({
                "notModified": true,
                "savedCount": 0,
                "contacts": [],
                "users": [],
            }),
        );
    }

    #[test]
    fn an_imported_contacts_document_carries_the_imports_and_retries() {
        assert_json(
            &ImportedContactsDto {
                imported: vec![ImportedContactDto {
                    user_id: 7,
                    client_id: 42,
                    user: Some(user()),
                    peer: Some(peer()),
                }],
                retry_contacts: vec![43],
                popular_invites: vec![PopularInviteDto {
                    client_id: 44,
                    importers: 3,
                }],
                users: vec![ContactUserDto {
                    user: user(),
                    peer: peer(),
                }],
            },
            serde_json::json!({
                "imported": [{
                    "userId": 7,
                    "clientId": 42,
                    "user": serde_json::to_value(user()).unwrap(),
                    "peer": serde_json::to_value(peer()).unwrap(),
                }],
                "retryContacts": [43],
                "popularInvites": [{"clientId": 44, "importers": 3}],
                "users": [{
                    "user": serde_json::to_value(user()).unwrap(),
                    "peer": serde_json::to_value(peer()).unwrap(),
                }],
            }),
        );
    }

    #[test]
    fn a_blocked_document_carries_the_count_and_the_dates() {
        assert_json(
            &BlockedContactsDto {
                count: Some(9),
                blocked: vec![BlockedPeerDto {
                    date: 1_700_000_000_000,
                    user: Some(user()),
                    peer: peer(),
                }],
                users: Vec::new(),
            },
            serde_json::json!({
                "count": 9,
                "blocked": [{
                    "date": 1_700_000_000_000i64,
                    "user": serde_json::to_value(user()).unwrap(),
                    "peer": serde_json::to_value(peer()).unwrap(),
                }],
                "users": [],
            }),
        );
    }

    #[test]
    fn a_found_document_carries_both_result_sets() {
        let entry = ContactPeerDto {
            user: Some(user()),
            peer: peer(),
        };
        assert_json(
            &FoundContactsDto {
                my_results: vec![entry],
                results: Vec::new(),
                users: Vec::new(),
            },
            serde_json::json!({
                "myResults": [{
                    "user": serde_json::to_value(user()).unwrap(),
                    "peer": serde_json::to_value(peer()).unwrap(),
                }],
                "results": [],
                "users": [],
            }),
        );
    }

    #[test]
    fn a_peer_document_omits_a_user_for_a_chat() {
        assert_json(
            &ContactPeerDto {
                user: None,
                peer: PeerDto {
                    native_handle: 3,
                    id: -1_000_007,
                    kind: "channel",
                    username: None,
                    name: Some("A Channel".to_owned()),
                    usernames: Vec::new(),
                    is_megagroup: None,
                    has_photo: false,
                    permissions: Some(ChatPermissionsDto::default()),
                },
            },
            serde_json::json!({
                "user": null,
                "peer": {
                    "nativeHandle": 3,
                    "id": -1_000_007,
                    "kind": "channel",
                    "username": null,
                    "name": "A Channel",
                    "usernames": [],
                    "isMegagroup": null,
                    "hasPhoto": false,
                    "permissions": serde_json::to_value(ChatPermissionsDto::default()).unwrap(),
                },
            }),
        );
    }

    #[test]
    fn the_layer_seconds_project_into_milliseconds() {
        assert_eq!(millis(1_700_000_000), 1_700_000_000_000);
        assert_eq!(millis(0), 0);
    }
}
