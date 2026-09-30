//! Chat rights projections: what an admin may do, and what a banned member may not.
//!
//! Both shapes are used in two directions. They are serialized when a participant or a peer
//! reports the rights Telegram holds, and deserialized when a caller asks for rights to be set,
//! where the fields of [`ChatPermissionsDto`] become the grants of `set_admin_rights` and the
//! fields of [`ChatRestrictionsDto`] the bans of `set_banned_rights`.
//!
//! The two spellings are deliberately kept apart. [`ChatPermissionsDto`] mirrors grammers'
//! `Permissions`, i.e. the layer's `chatAdminRights`, where `true` grants a right. Its counterpart
//! [`ChatRestrictionsDto`] mirrors grammers' `Restrictions`, i.e. the layer's `chatBannedRights`,
//! where `true` *denies* the same-named right. Everything that crosses the wire keeps the layer's
//! polarity, so a caller reading a restriction back sees exactly what it sent; the negation to the
//! "can do" spelling of the builder methods happens at the grammers call, not here.

use grammers_client::peer::{Permissions, Restrictions};
use grammers_client::tl;
use serde::{Deserialize, Serialize};

/// The admin rights of a chat member, mirroring grammers' [`Permissions`] (TL `chatAdminRights`).
///
/// Only the ten rights grammers exposes as accessors are projected. The layer's `other`,
/// `manage_topics` and story rights have no accessor, so they are absent rather than guessed.
///
/// A missing field decodes as its default, so a caller that only wants to grant one right does not
/// have to spell out the nine it does not.
#[derive(Serialize, Deserialize, Default)]
#[serde(default, rename_all = "camelCase")]
pub(crate) struct ChatPermissionsDto {
    pub(crate) change_info: bool,
    pub(crate) post_messages: bool,
    pub(crate) edit_messages: bool,
    pub(crate) delete_messages: bool,
    pub(crate) ban_users: bool,
    pub(crate) invite_users: bool,
    pub(crate) pin_messages: bool,
    pub(crate) add_admins: bool,
    pub(crate) anonymous: bool,
    pub(crate) manage_call: bool,
}

/// The restrictions applied to a banned or restricted member, mirroring grammers'
/// [`Restrictions`] (TL `chatBannedRights`).
///
/// A flag is `true` when the layer *bans* the right, which is the polarity grammers and Telegram
/// use; a caller that means "allowed" negates it. [`Self::until_date`] is epoch milliseconds, and
/// the epoch itself means "forever".
#[derive(Serialize, Deserialize, Default)]
#[serde(default, rename_all = "camelCase")]
pub(crate) struct ChatRestrictionsDto {
    pub(crate) view_messages: bool,
    pub(crate) send_messages: bool,
    pub(crate) send_media: bool,
    pub(crate) send_stickers: bool,
    pub(crate) send_gifs: bool,
    pub(crate) send_games: bool,
    pub(crate) send_inline: bool,
    pub(crate) embed_links: bool,
    pub(crate) send_polls: bool,
    pub(crate) change_info: bool,
    pub(crate) invite_users: bool,
    pub(crate) pin_messages: bool,
    /// Epoch milliseconds from `Restrictions::due()`; the epoch itself means "forever".
    pub(crate) until_date: i64,
}

impl From<&tl::types::ChatAdminRights> for ChatPermissionsDto {
    fn from(rights: &tl::types::ChatAdminRights) -> Self {
        Self {
            change_info: rights.change_info,
            post_messages: rights.post_messages,
            edit_messages: rights.edit_messages,
            delete_messages: rights.delete_messages,
            ban_users: rights.ban_users,
            invite_users: rights.invite_users,
            pin_messages: rights.pin_messages,
            add_admins: rights.add_admins,
            anonymous: rights.anonymous,
            manage_call: rights.manage_call,
        }
    }
}

impl From<&Permissions> for ChatPermissionsDto {
    fn from(permissions: &Permissions) -> Self {
        Self {
            change_info: permissions.change_info(),
            post_messages: permissions.post_messages(),
            edit_messages: permissions.edit_messages(),
            delete_messages: permissions.delete_messages(),
            ban_users: permissions.ban_users(),
            invite_users: permissions.invite_users(),
            pin_messages: permissions.pin_messages(),
            add_admins: permissions.add_admins(),
            anonymous: permissions.anonymous(),
            manage_call: permissions.manage_call(),
        }
    }
}

impl From<&tl::types::ChatBannedRights> for ChatRestrictionsDto {
    fn from(rights: &tl::types::ChatBannedRights) -> Self {
        Self {
            view_messages: rights.view_messages,
            send_messages: rights.send_messages,
            send_media: rights.send_media,
            send_stickers: rights.send_stickers,
            send_gifs: rights.send_gifs,
            send_games: rights.send_games,
            send_inline: rights.send_inline,
            embed_links: rights.embed_links,
            send_polls: rights.send_polls,
            change_info: rights.change_info,
            invite_users: rights.invite_users,
            pin_messages: rights.pin_messages,
            until_date: i64::from(rights.until_date) * 1_000,
        }
    }
}

impl From<&Restrictions> for ChatRestrictionsDto {
    fn from(restrictions: &Restrictions) -> Self {
        Self {
            view_messages: restrictions.view_messages(),
            send_messages: restrictions.send_messages(),
            send_media: restrictions.send_media(),
            send_stickers: restrictions.send_stickers(),
            send_gifs: restrictions.send_gifs(),
            send_games: restrictions.send_games(),
            send_inline: restrictions.send_inline(),
            embed_links: restrictions.embed_links(),
            send_polls: restrictions.send_polls(),
            change_info: restrictions.change_info(),
            invite_users: restrictions.invite_users(),
            pin_messages: restrictions.pin_messages(),
            until_date: restrictions.due().as_millisecond(),
        }
    }
}

/// The other direction: the grants a caller asked for, as the layer's `chatAdminRights`.
///
/// The layer's `other`, `manage_topics` and story rights are not exposed by this bridge, so they
/// are cleared rather than carried over.
impl From<&ChatPermissionsDto> for tl::types::ChatAdminRights {
    fn from(rights: &ChatPermissionsDto) -> Self {
        Self {
            change_info: rights.change_info,
            post_messages: rights.post_messages,
            edit_messages: rights.edit_messages,
            delete_messages: rights.delete_messages,
            ban_users: rights.ban_users,
            invite_users: rights.invite_users,
            pin_messages: rights.pin_messages,
            add_admins: rights.add_admins,
            anonymous: rights.anonymous,
            manage_call: rights.manage_call,
            other: false,
            manage_topics: false,
            post_stories: false,
            edit_stories: false,
            delete_stories: false,
            manage_direct_messages: false,
            manage_ranks: false,
            manage_linked_peers: false,
            manage_welcome_messages: false,
        }
    }
}

/// The other direction: the bans a caller asked for, as the layer's `chatBannedRights`.
///
/// The millisecond wire date is turned back into the layer's whole seconds; the layer's own
/// media-granular flags are left clear, exactly as grammers' `Restrictions` accessors cannot
/// report them.
impl From<&ChatRestrictionsDto> for tl::types::ChatBannedRights {
    fn from(rights: &ChatRestrictionsDto) -> Self {
        Self {
            view_messages: rights.view_messages,
            send_messages: rights.send_messages,
            send_media: rights.send_media,
            send_stickers: rights.send_stickers,
            send_gifs: rights.send_gifs,
            send_games: rights.send_games,
            send_inline: rights.send_inline,
            embed_links: rights.embed_links,
            send_polls: rights.send_polls,
            change_info: rights.change_info,
            invite_users: rights.invite_users,
            pin_messages: rights.pin_messages,
            manage_topics: false,
            send_photos: false,
            send_videos: false,
            send_roundvideos: false,
            send_audios: false,
            send_voices: false,
            send_docs: false,
            send_plain: false,
            edit_rank: false,
            send_reactions: false,
            manage_linked_peers: false,
            until_date: (rights.until_date / 1_000) as i32,
        }
    }
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the two request directions of the rights projections.

    use grammers_client::tl;
    use serde_json::json;

    use super::{ChatPermissionsDto, ChatRestrictionsDto};

    fn assert_json(dto: &impl serde::Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    #[test]
    fn a_requested_ban_keeps_the_layer_polarity_and_the_millisecond_date() {
        let dto = ChatRestrictionsDto {
            view_messages: true,
            send_messages: false,
            send_media: true,
            send_stickers: false,
            send_gifs: true,
            send_games: false,
            send_inline: true,
            embed_links: false,
            send_polls: true,
            change_info: false,
            invite_users: true,
            pin_messages: false,
            until_date: 1_700_000_000_000,
        };

        let raw = tl::types::ChatBannedRights::from(&dto);
        // The layer keeps the denied flags as given, and the millisecond date becomes seconds.
        assert!(raw.view_messages && !raw.send_messages && raw.send_media);
        assert_eq!(raw.until_date, 1_700_000_000);

        // Projecting back reproduces the exact document a caller sent.
        assert_json(
            &ChatRestrictionsDto::from(&raw),
            json!({
                "viewMessages": true,
                "sendMessages": false,
                "sendMedia": true,
                "sendStickers": false,
                "sendGifs": true,
                "sendGames": false,
                "sendInline": true,
                "embedLinks": false,
                "sendPolls": true,
                "changeInfo": false,
                "inviteUsers": true,
                "pinMessages": false,
                "untilDate": 1_700_000_000_000i64,
            }),
        );
    }

    #[test]
    fn a_permanent_ban_crosses_as_the_epoch() {
        let dto = ChatRestrictionsDto {
            send_messages: true,
            ..ChatRestrictionsDto::default()
        };
        let raw = tl::types::ChatBannedRights::from(&dto);
        assert_eq!(raw.until_date, 0);
        assert_eq!(ChatRestrictionsDto::from(&raw).until_date, 0);
    }

    #[test]
    fn a_requested_admin_grant_keeps_its_flags() {
        let dto = ChatPermissionsDto {
            change_info: true,
            post_messages: false,
            edit_messages: true,
            delete_messages: false,
            ban_users: true,
            invite_users: false,
            pin_messages: true,
            add_admins: false,
            anonymous: true,
            manage_call: false,
        };

        let raw = tl::types::ChatAdminRights::from(&dto);
        assert!(raw.change_info && !raw.post_messages && raw.edit_messages);
        assert!(raw.anonymous && !raw.manage_call);
        // The layer rights this bridge does not model stay clear.
        assert!(!raw.other && !raw.manage_topics && !raw.post_stories);

        assert_json(
            &ChatPermissionsDto::from(&raw),
            json!({
                "changeInfo": true,
                "postMessages": false,
                "editMessages": true,
                "deleteMessages": false,
                "banUsers": true,
                "inviteUsers": false,
                "pinMessages": true,
                "addAdmins": false,
                "anonymous": true,
                "manageCall": false,
            }),
        );
    }

    #[test]
    fn a_partial_request_decodes_with_the_missing_flags_cleared() {
        let dto: ChatRestrictionsDto =
            serde_json::from_str(r#"{"sendMessages": true}"#).expect("a partial ban");
        assert!(dto.send_messages);
        assert!(!dto.view_messages);
        assert_eq!(dto.until_date, 0);
    }
}
