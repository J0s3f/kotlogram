//! Request payload shapes shared by more than one operation module.

use serde::Deserialize;

/// Peer selector: either a handle handed out by a previous result, or a public username.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PeerTarget {
    pub(crate) peer_handle: Option<i64>,
    pub(crate) username: Option<String>,
}

/// Result-limit selector used by the paged listings.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct LimitPayload {
    pub(crate) limit: Option<usize>,
}
