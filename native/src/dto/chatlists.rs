//! Chatlist-sharing projection.
//!
//! The `chatlists.*` answers all pair a bare `Peer` vector with the `chats` and `users` objects
//! that describe it, exactly as the `contacts.*` answers do, so a peer is resolved against the same
//! answer rather than against Telegram. A peer naming no object in the answer is skipped: it has no
//! access hash to register and so could not be sent back.
//!
//! Two answers carry no bundle of their own. `chatlists.getLeaveChatlistSuggestions` reports bare
//! peers alone, so those are projected unresolved, by their Bot API dialog id and kind, and a caller
//! reads them only to name the peers of a `leaveChatlist` call. The three `Updates` answers are
//! reduced to the chats they report plus an acknowledgement: the point updates themselves already
//! reach a caller through the update stream, so this projection does not duplicate them.

use grammers_client::peer::{Peer, User as ClientUser};
use grammers_client::tl;
use grammers_session::types::{PeerId, PeerKind};
use serde::Serialize;

use crate::client::NativeClient;
use crate::dto::message::{message_entity_dto, MessageEntityDto};
use crate::dto::peer::{peer_dto, PeerDto};
use crate::dto::user::{user_dto, UserDto};

/// A bare peer the answer carried no object for.
///
/// Only [`crate::ops::chatlists`]'s leave suggestions reach this: the other answers always carry the
/// chats and users that describe their peers, which are projected in full as [`PeerDto`].
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ChatlistPeerDto {
    /// The Bot API dialog id, or `0` for a peer with no identifier.
    pub(crate) id: i64,
    /// `user`, `chat` or `channel`, which is how the layer splits `Peer`.
    pub(crate) kind: &'static str,
}

/// One exported invite of a folder.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ExportedInviteDto {
    /// The invite's label, which is a plain string on this answer rather than the rich text an
    /// unjoined invite carries.
    pub(crate) title: String,
    /// The slug the invite is addressed by, which is what `deleteExportedInvite` and
    /// `editExportedInvite` take. The layer spells it inside the join URL.
    pub(crate) slug: String,
    /// The full join URL a caller shares.
    pub(crate) url: String,
    /// The peers the invite covers. The layer reports them as bare references with no objects of
    /// their own on this answer, so they are projected by dialog id and kind rather than resolved.
    pub(crate) peers: Vec<ChatlistPeerDto>,
}

/// The `chatlistsGetExportedInvites` document: the folder's invites and the objects they name.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ExportedInvitesDto {
    pub(crate) invites: Vec<ExportedInviteDto>,
    /// The chats every invite's peer lists reference, projected once for the whole answer.
    pub(crate) chats: Vec<PeerDto>,
    pub(crate) users: Vec<UserDto>,
}

/// An unjoined invite, as `chatlistsCheckChatlistInvite` reports a slug that can still be joined.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct NewChatlistInviteDto {
    /// True when the title should not animate on arrival, which the layer carries as a flag.
    pub(crate) title_noanimate: bool,
    /// The plain text of the invite's `TextWithEntities` title.
    pub(crate) title: String,
    /// The formatting entities on that title, in the same shape a message's entities take.
    pub(crate) title_entities: Vec<MessageEntityDto>,
    /// The invite's emoji, when the layer carries one.
    pub(crate) emoticon: Option<String>,
    /// The peers the folder holds, resolved against the answer's own objects.
    pub(crate) peers: Vec<PeerDto>,
    pub(crate) chats: Vec<PeerDto>,
    pub(crate) users: Vec<UserDto>,
}

/// An invite to a folder the account has already joined, as `chatlistsCheckChatlistInvite` reports
/// it: the folder's id, which peers are already in, and which joining would add.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct JoinedChatlistInviteDto {
    /// The dialog filter the slug resolves to, which is the id an `InputChatlist` names.
    pub(crate) filter_id: i32,
    /// The peers the folder still misses, which are what a `joinChatlistInvite` call names.
    pub(crate) missing_peers: Vec<PeerDto>,
    /// The peers already in the folder.
    pub(crate) already_peers: Vec<PeerDto>,
    pub(crate) chats: Vec<PeerDto>,
    pub(crate) users: Vec<UserDto>,
}

/// The two distinct answers to `chatlistsCheckChatlistInvite`.
///
/// The layer's `chatlists.chatlistInvite` and `chatlists.chatlistInviteAlready` say different
/// things: the first is a still-unjoined invite with a title and a peer roster, the second names
/// the folder the slug already points at and sorts the peers into missing and already-joined. They
/// are kept as two variants here rather than flattened, so a caller can tell which case it holds.
#[derive(Serialize)]
#[serde(tag = "kind", rename_all = "camelCase")]
pub(crate) enum ChatlistInviteDto {
    /// A still-unjoined invite, named `new` because joining would create the folder.
    #[serde(rename = "new")]
    New(NewChatlistInviteDto),
    /// An invite to a folder the account is already in.
    #[serde(rename = "already")]
    Already(JoinedChatlistInviteDto),
}

/// The `chatlistsGetChatlistUpdates` document: the peers a joined folder still misses and the
/// objects that describe them.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ChatlistUpdatesDto {
    /// The peers the folder holds that this account is not part of yet.
    pub(crate) missing_peers: Vec<PeerDto>,
    pub(crate) chats: Vec<PeerDto>,
    pub(crate) users: Vec<UserDto>,
}

/// The result of the three `Updates`-returning calls: an acknowledgement plus the peers the call
/// touched.
///
/// `joinChatlistInvite`, `joinChatlistUpdates` and `leaveChatlist` all answer an `Updates` bundle.
/// The point updates it carries are already delivered to a caller by the update stream, so only the
/// chats the bundle names are projected here — the folder members that were joined or left — with
/// [`Self::ok`] standing for the call itself.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ChatlistUpdatesAckDto {
    pub(crate) ok: bool,
    /// The chats the bundle reports, projected and registered.
    pub(crate) peers: Vec<PeerDto>,
}

/// Projects one exported invite from the layer's own `exportedChatlistInvite`.
///
/// The invite carries bare `Peer` values and no objects of its own; where the answer has chats and
/// users (`chatlistsGetExportedInvites`) they are projected separately at the document level.
pub(crate) fn exported_invite_dto(
    invite: &tl::types::ExportedChatlistInvite,
) -> Result<ExportedInviteDto, String> {
    Ok(ExportedInviteDto {
        title: invite.title.clone(),
        slug: slug_of(&invite.url)?,
        url: invite.url.clone(),
        peers: bare_peers_dto(&invite.peers),
    })
}

/// Projects a whole `chatlists.exportedInvites` answer.
pub(crate) fn exported_invites_dto(
    native: &NativeClient,
    result: &tl::types::chatlists::ExportedInvites,
) -> Result<ExportedInvitesDto, String> {
    let mut invites = Vec::with_capacity(result.invites.len());
    for invite in &result.invites {
        let tl::enums::ExportedChatlistInvite::Invite(invite) = invite;
        invites.push(exported_invite_dto(invite)?);
    }
    Ok(ExportedInvitesDto {
        invites,
        chats: chats_dto(native, &result.chats)?,
        users: users_dto(native, &result.users),
    })
}

/// Projects the answer to `chatlistsCheckChatlistInvite`, keeping the layer's two cases apart.
pub(crate) fn chatlist_invite_dto(
    native: &NativeClient,
    result: &tl::enums::chatlists::ChatlistInvite,
) -> Result<ChatlistInviteDto, String> {
    Ok(match result {
        tl::enums::chatlists::ChatlistInvite::Invite(invite) => {
            let tl::enums::TextWithEntities::Entities(title) = &invite.title;
            ChatlistInviteDto::New(NewChatlistInviteDto {
                title_noanimate: invite.title_noanimate,
                title: title.text.clone(),
                title_entities: title.entities.iter().map(message_entity_dto).collect(),
                emoticon: invite.emoticon.clone(),
                peers: peers_dto(native, &invite.peers, &invite.chats, &invite.users)?,
                chats: chats_dto(native, &invite.chats)?,
                users: users_dto(native, &invite.users),
            })
        }
        tl::enums::chatlists::ChatlistInvite::Already(invite) => {
            ChatlistInviteDto::Already(JoinedChatlistInviteDto {
                filter_id: invite.filter_id,
                missing_peers: peers_dto(
                    native,
                    &invite.missing_peers,
                    &invite.chats,
                    &invite.users,
                )?,
                already_peers: peers_dto(
                    native,
                    &invite.already_peers,
                    &invite.chats,
                    &invite.users,
                )?,
                chats: chats_dto(native, &invite.chats)?,
                users: users_dto(native, &invite.users),
            })
        }
    })
}

/// Projects a whole `chatlists.chatlistUpdates` answer.
pub(crate) fn chatlist_updates_dto(
    native: &NativeClient,
    result: &tl::types::chatlists::ChatlistUpdates,
) -> Result<ChatlistUpdatesDto, String> {
    Ok(ChatlistUpdatesDto {
        missing_peers: peers_dto(
            native,
            &result.missing_peers,
            &result.chats,
            &result.users,
        )?,
        chats: chats_dto(native, &result.chats)?,
        users: users_dto(native, &result.users),
    })
}

/// Projects bare layer peers by their Bot API dialog id and kind, with no resolution.
///
/// Used where the answer carries no chats or users to resolve against: an exported invite's own
/// peer list and the leave suggestions.
fn bare_peers_dto(peers: &[tl::enums::Peer]) -> Vec<ChatlistPeerDto> {
    peers
        .iter()
        .map(|peer| {
            let id = PeerId::from(peer);
            let kind = match id.kind() {
                PeerKind::User => "user",
                PeerKind::Chat => "chat",
                PeerKind::Channel => "channel",
            };
            ChatlistPeerDto {
                id: id.bot_api_dialog_id().unwrap_or(0),
                kind,
            }
        })
        .collect()
}

/// Projects the leave suggestions, which the layer reports as bare peers with no objects.
pub(crate) fn leave_suggestions_dto(peers: &[tl::enums::Peer]) -> Vec<ChatlistPeerDto> {
    bare_peers_dto(peers)
}

/// Reduces an `Updates` bundle to an acknowledgement plus the chats it reports.
///
/// The point updates are not projected: the update stream already hands them to a caller, so
/// carrying them here would duplicate the typed update surface. The chats are, because they are the
/// folder members the call acted on.
pub(crate) fn updates_ack_dto(
    native: &NativeClient,
    updates: &tl::enums::Updates,
) -> Result<ChatlistUpdatesAckDto, String> {
    let chats = match updates {
        tl::enums::Updates::Combined(bundle) => &bundle.chats,
        tl::enums::Updates::Updates(bundle) => &bundle.chats,
        _ => &[][..],
    };
    Ok(ChatlistUpdatesAckDto {
        ok: true,
        peers: chats_dto(native, chats)?,
    })
}

/// The slug of an invite, read from the join URL the layer reports it inside.
///
/// The layer has no slug field on `exportedChatlistInvite`: the slug is the tail of the join URL,
/// which Telegram shapes as `https://t.me/+<slug>` or the older `https://t.me/joinchat/<slug>`. A
/// URL of neither shape is refused rather than projected with the wrong segment, because the slug
/// is what every other invite call is addressed by.
pub(crate) fn slug_of(url: &str) -> Result<String, String> {
    let tail = url
        .split_once("/+")
        .map(|(_, tail)| tail)
        .or_else(|| url.split_once("joinchat/").map(|(_, tail)| tail));
    let Some(tail) = tail else {
        return Err(format!("the invite URL {url} carries no slug"));
    };
    // A trailing query or fragment would otherwise ride along with the slug.
    let slug = tail.split(['?', '#', '/']).next().unwrap_or("");
    if slug.is_empty() {
        return Err(format!("the invite URL {url} carries no slug"));
    }
    Ok(slug.to_owned())
}

/// Resolves each layer peer against the answer's own chats and users and registers it.
///
/// A peer naming no object is skipped rather than projected without a handle, the same way the
/// contacts projections treat a missing object.
fn peers_dto(
    native: &NativeClient,
    peers: &[tl::enums::Peer],
    chats: &[tl::enums::Chat],
    users: &[tl::enums::User],
) -> Result<Vec<PeerDto>, String> {
    let mut rows = Vec::with_capacity(peers.len());
    for peer in peers {
        let id = PeerId::from(peer);
        let Some(bare) = id.bare_id() else {
            continue;
        };
        let resolved = match id.kind() {
            PeerKind::User => users
                .iter()
                .find(|user| user.id() == bare)
                .cloned()
                .map(|user| Peer::User(ClientUser::from_raw(&native.client, user))),
            PeerKind::Chat | PeerKind::Channel => chats
                .iter()
                .find(|chat| chat.id() == bare)
                .cloned()
                .map(|chat| Peer::from_raw(&native.client, chat)),
        };
        let Some(resolved) = resolved else {
            continue;
        };
        rows.push(peer_dto(native, &resolved)?);
    }
    Ok(rows)
}

/// Projects an answer's chat vector, each chat resolved through its own object.
fn chats_dto(native: &NativeClient, chats: &[tl::enums::Chat]) -> Result<Vec<PeerDto>, String> {
    let mut rows = Vec::with_capacity(chats.len());
    for chat in chats {
        rows.push(peer_dto(native, &Peer::from_raw(&native.client, chat.clone()))?);
    }
    Ok(rows)
}

/// Projects an answer's user vector.
fn users_dto(native: &NativeClient, users: &[tl::enums::User]) -> Vec<UserDto> {
    users
        .iter()
        .map(|user| user_dto(&ClientUser::from_raw(&native.client, user.clone())))
        .collect()
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the chatlist documents.
    //!
    //! The projections themselves need a live grammers client, so what is pinned here is the slug
    //! extraction, the leave-suggestion projection and the exact document each constructor encodes.

    use super::*;
    use crate::dto::permissions::ChatPermissionsDto;
    use serde::Serialize;
    use tl::enums::Peer as TlPeer;

    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
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

    fn chatlist_peer() -> ChatlistPeerDto {
        ChatlistPeerDto {
            id: 7,
            kind: "user",
        }
    }

    #[test]
    fn a_slug_is_read_from_the_join_url() {
        assert_eq!(slug_of("https://t.me/+AbCdEf").expect("a slug"), "AbCdEf");
        assert_eq!(
            slug_of("https://t.me/joinchat/AbCdEf").expect("a slug"),
            "AbCdEf"
        );
        assert_eq!(
            slug_of("https://t.me/+AbCdEf?single").expect("a slug"),
            "AbCdEf"
        );
        assert_eq!(
            slug_of("https://t.me/+AbCdEf#fragment").expect("a slug"),
            "AbCdEf"
        );
    }

    #[test]
    fn a_url_without_a_slug_is_refused() {
        let error = slug_of("https://t.me/").expect_err("no slug");
        assert!(error.contains("carries no slug"), "{error}");
        let error = slug_of("").expect_err("no slug");
        assert!(error.contains("carries no slug"), "{error}");
    }

    #[test]
    fn a_leave_suggestion_carries_its_dialog_id_and_kind() {
        let suggestions = leave_suggestions_dto(&[
            TlPeer::User(tl::types::PeerUser { user_id: 42 }),
            TlPeer::Chat(tl::types::PeerChat { chat_id: 7 }),
            TlPeer::Channel(tl::types::PeerChannel {
                channel_id: 1_000_007,
            }),
        ]);
        assert_eq!(suggestions.len(), 3);
        assert_eq!(suggestions[0].id, 42);
        assert_eq!(suggestions[0].kind, "user");
        assert_eq!(suggestions[1].id, -7);
        assert_eq!(suggestions[1].kind, "chat");
        assert_eq!(suggestions[2].id, -1_000_001_000_007);
        assert_eq!(suggestions[2].kind, "channel");
    }

    #[test]
    fn an_exported_invite_document_carries_its_title_slug_url_and_peers() {
        assert_json(
            &ExportedInviteDto {
                title: "News".to_owned(),
                slug: "AbCdEf".to_owned(),
                url: "https://t.me/+AbCdEf".to_owned(),
                peers: vec![chatlist_peer()],
            },
            serde_json::json!({
                "title": "News",
                "slug": "AbCdEf",
                "url": "https://t.me/+AbCdEf",
                "peers": [serde_json::to_value(chatlist_peer()).unwrap()],
            }),
        );
    }

    #[test]
    fn an_exported_invites_document_carries_the_invites_and_the_objects() {
        assert_json(
            &ExportedInvitesDto {
                invites: vec![ExportedInviteDto {
                    title: "News".to_owned(),
                    slug: "AbCdEf".to_owned(),
                    url: "https://t.me/+AbCdEf".to_owned(),
                    peers: Vec::new(),
                }],
                chats: Vec::new(),
                users: Vec::new(),
            },
            serde_json::json!({
                "invites": [{
                    "title": "News",
                    "slug": "AbCdEf",
                    "url": "https://t.me/+AbCdEf",
                    "peers": [],
                }],
                "chats": [],
                "users": [],
            }),
        );
    }

    #[test]
    fn a_new_invite_document_carries_its_title_and_entities() {
        assert_json(
            &ChatlistInviteDto::New(NewChatlistInviteDto {
                title_noanimate: true,
                title: "News".to_owned(),
                title_entities: vec![MessageEntityDto::plain("bold", 0, 4)],
                emoticon: Some("📰".to_owned()),
                peers: vec![peer()],
                chats: Vec::new(),
                users: Vec::new(),
            }),
            serde_json::json!({
                "kind": "new",
                "titleNoanimate": true,
                "title": "News",
                "titleEntities": [{
                    "type": "bold",
                    "offset": 0,
                    "length": 4,
                    "url": null,
                    "userId": null,
                    "language": null,
                    "customEmojiId": null,
                }],
                "emoticon": "📰",
                "peers": [serde_json::to_value(peer()).unwrap()],
                "chats": [],
                "users": [],
            }),
        );
    }

    #[test]
    fn an_already_joined_invite_document_names_the_folder_and_sorts_the_peers() {
        assert_json(
            &ChatlistInviteDto::Already(JoinedChatlistInviteDto {
                filter_id: 4,
                missing_peers: vec![peer()],
                already_peers: Vec::new(),
                chats: Vec::new(),
                users: Vec::new(),
            }),
            serde_json::json!({
                "kind": "already",
                "filterId": 4,
                "missingPeers": [serde_json::to_value(peer()).unwrap()],
                "alreadyPeers": [],
                "chats": [],
                "users": [],
            }),
        );
    }

    #[test]
    fn a_chatlist_updates_document_carries_the_missing_peers_and_the_objects() {
        assert_json(
            &ChatlistUpdatesDto {
                missing_peers: vec![peer()],
                chats: Vec::new(),
                users: Vec::new(),
            },
            serde_json::json!({
                "missingPeers": [serde_json::to_value(peer()).unwrap()],
                "chats": [],
                "users": [],
            }),
        );
    }

    #[test]
    fn an_updates_acknowledgement_carries_the_ok_flag_and_the_peers() {
        assert_json(
            &ChatlistUpdatesAckDto {
                ok: true,
                peers: vec![peer()],
            },
            serde_json::json!({
                "ok": true,
                "peers": [serde_json::to_value(peer()).unwrap()],
            }),
        );
    }

    #[test]
    fn a_channel_peer_document_carries_its_handle() {
        assert_json(
            &ChatlistPeerDto {
                id: -1_000_007,
                kind: "channel",
            },
            serde_json::json!({ "id": -1_000_007, "kind": "channel" }),
        );
        // Keeps the permissions import meaningful for the shared DTO fixture.
        let _ = ChatPermissionsDto::default();
    }
}
