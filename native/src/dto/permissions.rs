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
//!
//! Every flag the layer carries is projected, including the ones grammers' `Permissions` and
//! `Restrictions` accessors do not name: those wrappers keep the full raw TL struct in their public
//! `raw` field, and the projections read it directly rather than going through the ten accessors.

use grammers_client::peer::{Permissions, Restrictions};
use grammers_client::tl;
use serde::{Deserialize, Serialize};

/// The admin rights of a chat member, mirroring grammers' [`Permissions`] (TL `chatAdminRights`).
///
/// Every right the pinned layer carries is projected, not just the ten grammers exposes as
/// accessors. A missing field decodes as its default, so a caller that only wants to grant one
/// right does not have to spell out the rest.
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
    pub(crate) manage_topics: bool,
    pub(crate) post_stories: bool,
    pub(crate) edit_stories: bool,
    pub(crate) delete_stories: bool,
    pub(crate) manage_direct_messages: bool,
    pub(crate) manage_ranks: bool,
    pub(crate) manage_linked_peers: bool,
    pub(crate) manage_welcome_messages: bool,
    pub(crate) other: bool,
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
    pub(crate) manage_topics: bool,
    pub(crate) send_photos: bool,
    pub(crate) send_videos: bool,
    pub(crate) send_roundvideos: bool,
    pub(crate) send_audios: bool,
    pub(crate) send_voices: bool,
    pub(crate) send_docs: bool,
    pub(crate) send_plain: bool,
    pub(crate) edit_rank: bool,
    pub(crate) send_reactions: bool,
    pub(crate) manage_linked_peers: bool,
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
            manage_topics: rights.manage_topics,
            post_stories: rights.post_stories,
            edit_stories: rights.edit_stories,
            delete_stories: rights.delete_stories,
            manage_direct_messages: rights.manage_direct_messages,
            manage_ranks: rights.manage_ranks,
            manage_linked_peers: rights.manage_linked_peers,
            manage_welcome_messages: rights.manage_welcome_messages,
            other: rights.other,
        }
    }
}

/// Projects grammers' accessor wrapper by reading the full raw rights it carries; the ten
/// accessors would drop the newer flags.
impl From<&Permissions> for ChatPermissionsDto {
    fn from(permissions: &Permissions) -> Self {
        Self::from(&permissions.raw)
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
            manage_topics: rights.manage_topics,
            send_photos: rights.send_photos,
            send_videos: rights.send_videos,
            send_roundvideos: rights.send_roundvideos,
            send_audios: rights.send_audios,
            send_voices: rights.send_voices,
            send_docs: rights.send_docs,
            send_plain: rights.send_plain,
            edit_rank: rights.edit_rank,
            send_reactions: rights.send_reactions,
            manage_linked_peers: rights.manage_linked_peers,
            until_date: i64::from(rights.until_date) * 1_000,
        }
    }
}

/// Projects grammers' accessor wrapper by reading the full raw rights it carries; the accessors
/// would drop the newer flags.
impl From<&Restrictions> for ChatRestrictionsDto {
    fn from(restrictions: &Restrictions) -> Self {
        Self::from(&restrictions.raw)
    }
}

/// The other direction: the grants a caller asked for, as the layer's `chatAdminRights`.
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
            other: rights.other,
            manage_topics: rights.manage_topics,
            post_stories: rights.post_stories,
            edit_stories: rights.edit_stories,
            delete_stories: rights.delete_stories,
            manage_direct_messages: rights.manage_direct_messages,
            manage_ranks: rights.manage_ranks,
            manage_linked_peers: rights.manage_linked_peers,
            manage_welcome_messages: rights.manage_welcome_messages,
        }
    }
}

/// The other direction: the bans a caller asked for, as the layer's `chatBannedRights`.
///
/// The millisecond wire date is turned back into the layer's whole seconds.
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
            manage_topics: rights.manage_topics,
            send_photos: rights.send_photos,
            send_videos: rights.send_videos,
            send_roundvideos: rights.send_roundvideos,
            send_audios: rights.send_audios,
            send_voices: rights.send_voices,
            send_docs: rights.send_docs,
            send_plain: rights.send_plain,
            edit_rank: rights.edit_rank,
            send_reactions: rights.send_reactions,
            manage_linked_peers: rights.manage_linked_peers,
            until_date: (rights.until_date / 1_000) as i32,
        }
    }
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the two request directions of the rights projections, plus the
    //! projection of grammers' accessor wrappers from the raw rights they carry.

    use grammers_client::peer::{Permissions, Restrictions};
    use grammers_client::tl;
    use serde_json::json;

    use super::{ChatPermissionsDto, ChatRestrictionsDto};

    fn assert_json(dto: &impl serde::Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    fn raw_admin_rights() -> tl::types::ChatAdminRights {
        tl::types::ChatAdminRights {
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
            other: true,
            manage_topics: true,
            post_stories: true,
            edit_stories: true,
            delete_stories: true,
            manage_direct_messages: true,
            manage_ranks: true,
            manage_linked_peers: true,
            manage_welcome_messages: true,
        }
    }

    fn raw_banned_rights() -> tl::types::ChatBannedRights {
        tl::types::ChatBannedRights {
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
            manage_topics: true,
            send_photos: true,
            send_videos: true,
            send_roundvideos: true,
            send_audios: true,
            send_voices: true,
            send_docs: true,
            send_plain: true,
            edit_rank: true,
            send_reactions: true,
            manage_linked_peers: true,
            until_date: 1_700_000_000,
        }
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
            manage_topics: true,
            send_photos: true,
            send_videos: false,
            send_roundvideos: true,
            send_audios: false,
            send_voices: true,
            send_docs: false,
            send_plain: true,
            edit_rank: false,
            send_reactions: true,
            manage_linked_peers: false,
            until_date: 1_700_000_000_000,
        };

        let raw = tl::types::ChatBannedRights::from(&dto);
        // The layer keeps the denied flags as given, and the millisecond date becomes seconds.
        assert!(raw.view_messages && !raw.send_messages && raw.send_media);
        assert!(raw.manage_topics && raw.send_photos && !raw.send_videos);
        assert!(raw.send_reactions && !raw.manage_linked_peers);
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
                "manageTopics": true,
                "sendPhotos": true,
                "sendVideos": false,
                "sendRoundvideos": true,
                "sendAudios": false,
                "sendVoices": true,
                "sendDocs": false,
                "sendPlain": true,
                "editRank": false,
                "sendReactions": true,
                "manageLinkedPeers": false,
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
            manage_topics: true,
            post_stories: false,
            edit_stories: true,
            delete_stories: false,
            manage_direct_messages: true,
            manage_ranks: false,
            manage_linked_peers: true,
            manage_welcome_messages: false,
            other: true,
        };

        let raw = tl::types::ChatAdminRights::from(&dto);
        assert!(raw.change_info && !raw.post_messages && raw.edit_messages);
        assert!(raw.anonymous && !raw.manage_call);
        assert!(raw.manage_topics && !raw.post_stories && raw.edit_stories);
        assert!(raw.manage_direct_messages && raw.manage_linked_peers && raw.other);

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
                "manageTopics": true,
                "postStories": false,
                "editStories": true,
                "deleteStories": false,
                "manageDirectMessages": true,
                "manageRanks": false,
                "manageLinkedPeers": true,
                "manageWelcomeMessages": false,
                "other": true,
            }),
        );
    }

    #[test]
    fn every_new_admin_flag_round_trips_through_the_layer_struct() {
        let raw = raw_admin_rights();
        let dto = ChatPermissionsDto::from(&raw);
        assert!(
            dto.other
                && dto.manage_topics
                && dto.post_stories
                && dto.edit_stories
                && dto.delete_stories
                && dto.manage_direct_messages
                && dto.manage_ranks
                && dto.manage_linked_peers
                && dto.manage_welcome_messages
        );

        let back = tl::types::ChatAdminRights::from(&dto);
        assert!(back.other && back.manage_topics && back.post_stories && back.edit_stories);
        assert!(back.delete_stories && back.manage_direct_messages && back.manage_ranks);
        assert!(back.manage_linked_peers && back.manage_welcome_messages);
    }

    #[test]
    fn every_new_ban_flag_round_trips_through_the_layer_struct() {
        let raw = raw_banned_rights();
        let dto = ChatRestrictionsDto::from(&raw);
        assert!(
            dto.manage_topics
                && dto.send_photos
                && dto.send_videos
                && dto.send_roundvideos
                && dto.send_audios
                && dto.send_voices
                && dto.send_docs
                && dto.send_plain
                && dto.edit_rank
                && dto.send_reactions
                && dto.manage_linked_peers
        );

        let back = tl::types::ChatBannedRights::from(&dto);
        assert!(back.manage_topics && back.send_photos && back.send_videos);
        assert!(back.send_roundvideos && back.send_audios && back.send_voices);
        assert!(back.send_docs && back.send_plain && back.edit_rank);
        assert!(back.send_reactions && back.manage_linked_peers);
        assert_eq!(back.until_date, 1_700_000_000);
    }

    #[test]
    fn a_grammers_accessor_wrapper_projects_every_raw_admin_flag() {
        // `Permissions`/`Restrictions` only expose ten accessors, so the projection must read the
        // public raw struct instead of the accessor surface.
        let dto = ChatPermissionsDto::from(&Permissions {
            raw: raw_admin_rights(),
        });
        assert!(dto.other && dto.manage_topics && dto.post_stories && dto.edit_stories);
        assert!(dto.delete_stories && dto.manage_direct_messages && dto.manage_ranks);
        assert!(dto.manage_linked_peers && dto.manage_welcome_messages);
    }

    #[test]
    fn a_grammers_accessor_wrapper_projects_every_raw_ban_flag() {
        let dto = ChatRestrictionsDto::from(&Restrictions {
            raw: raw_banned_rights(),
        });
        assert!(dto.manage_topics && dto.send_photos && dto.send_videos);
        assert!(dto.send_roundvideos && dto.send_audios && dto.send_voices);
        assert!(dto.send_docs && dto.send_plain && dto.edit_rank);
        assert!(dto.send_reactions && dto.manage_linked_peers);
        assert_eq!(dto.until_date, 1_700_000_000_000);
    }

    #[test]
    fn a_partial_request_decodes_with_the_missing_flags_cleared() {
        let dto: ChatRestrictionsDto =
            serde_json::from_str(r#"{"sendMessages": true}"#).expect("a partial ban");
        assert!(dto.send_messages);
        assert!(!dto.view_messages);
        assert!(!dto.manage_topics && !dto.send_photos && !dto.manage_linked_peers);
        assert_eq!(dto.until_date, 0);
    }
}
