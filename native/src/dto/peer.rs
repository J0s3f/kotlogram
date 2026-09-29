//! Peer projection.

use grammers_client::types::Peer;
use serde::Serialize;

use crate::client::{register_peer, NativeClient};
use crate::dto::permissions::ChatPermissionsDto;

/// A resolved chat, user or broadcast channel.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PeerDto {
    pub(crate) native_handle: i64,
    pub(crate) id: i64,
    /// `user`, `group` or `channel`, which is how grammers splits [`Peer`].
    pub(crate) kind: &'static str,
    pub(crate) username: Option<String>,
    pub(crate) name: Option<String>,
    /// The collectible usernames, which grammers reports separately from [Self::username].
    pub(crate) usernames: Vec<String>,
    /// True for a megagroup, `None` for anything that is not a group chat.
    pub(crate) is_megagroup: Option<bool>,
    pub(crate) has_photo: bool,
    /// The logged-in account's admin rights. grammers only reports these for a broadcast
    /// channel, so a group or a user has `None`.
    pub(crate) permissions: Option<ChatPermissionsDto>,
}

/// Registers [peer] and describes it. The handle stays valid for the lifetime of the client.
pub(crate) fn peer_dto(native: &NativeClient, peer: &Peer) -> Result<PeerDto, String> {
    let native_handle = register_peer(native, peer)?;

    let kind = match peer {
        Peer::User(_) => "user",
        Peer::Group(_) => "group",
        Peer::Channel(_) => "channel",
    };
    let (is_megagroup, permissions) = match peer {
        Peer::User(_) => (None, None),
        Peer::Group(group) => (Some(group.is_megagroup()), None),
        Peer::Channel(channel) => (None, channel.admin_rights().map(ChatPermissionsDto::from)),
    };
    Ok(PeerDto {
        native_handle,
        id: peer.id().bot_api_dialog_id(),
        kind,
        username: peer.username().map(ToOwned::to_owned),
        name: peer.name().map(ToOwned::to_owned),
        usernames: peer
            .usernames()
            .into_iter()
            .map(ToOwned::to_owned)
            .collect(),
        is_megagroup,
        has_photo: peer.photo(false).is_some(),
        permissions,
    })
}
