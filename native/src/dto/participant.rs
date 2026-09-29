//! Chat-participant projection.

use grammers_client::client::chats::ParticipantPermissions as ClientParticipantPermissions;
use grammers_client::types::{Participant as ClientParticipant, Role};
use serde::Serialize;

use crate::dto::permissions::{ChatPermissionsDto, ChatRestrictionsDto};
use crate::dto::user::{user_dto, UserDto};

/// A chat member and the role grammers reports for it.
///
/// A role is one of five variants and they share no accessors, so every role-specific field is
/// `None` for the roles that cannot answer it. A creator has no date, only a creator and an admin
/// carry rights, and only a banned member carries restrictions.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ParticipantDto {
    pub(crate) user: UserDto,
    /// `member`, `creator`, `admin`, `banned`, `left`, or `unknown` for a variant this bridge
    /// does not model yet.
    pub(crate) role: &'static str,
    /// The custom admin title Telegram shows instead of the role name.
    pub(crate) rank: Option<String>,
    /// Epoch milliseconds, from the role's join, promotion or ban date.
    pub(crate) date: Option<i64>,
    /// The identifier of the user who added this member, which is all grammers reports.
    pub(crate) invited_by: Option<i64>,
    pub(crate) promoted_by: Option<i64>,
    pub(crate) kicked_by: Option<i64>,
    pub(crate) can_edit: Option<bool>,
    /// True when a banned participant has already left the chat on their own.
    pub(crate) left: Option<bool>,
    pub(crate) permissions: Option<ChatPermissionsDto>,
    pub(crate) restrictions: Option<ChatRestrictionsDto>,
}

/// Projects a grammers chat participant.
pub(crate) fn participant_dto(participant: &ClientParticipant) -> ParticipantDto {
    let mut dto = ParticipantDto {
        user: user_dto(&participant.user),
        role: role_name(&participant.role),
        rank: None,
        date: None,
        invited_by: None,
        promoted_by: None,
        kicked_by: None,
        can_edit: None,
        left: None,
        permissions: None,
        restrictions: None,
    };

    match &participant.role {
        Role::User(role) => {
            dto.date = Some(role.date().timestamp_millis());
            dto.invited_by = role.inviter_id();
        }
        Role::Creator(role) => {
            dto.permissions = Some(role.permissions().into());
            dto.rank = role.rank().map(ToOwned::to_owned);
        }
        Role::Admin(role) => {
            dto.can_edit = Some(role.can_edit());
            dto.invited_by = role.inviter_id();
            dto.promoted_by = role.promoted_by();
            dto.date = Some(role.date().timestamp_millis());
            dto.permissions = Some(role.permissions().into());
            dto.rank = role.rank().map(ToOwned::to_owned);
        }
        Role::Banned(role) => {
            dto.left = Some(role.left());
            dto.kicked_by = Some(role.kicked_by());
            dto.date = Some(role.date().timestamp_millis());
            dto.restrictions = Some(role.restrictions().into());
        }
        Role::Left(_) => {}
        // `Role` is `#[non_exhaustive]`: an unknown role keeps the `unknown` name [role_name]
        // gave it and no other detail.
        _ => {}
    }

    dto
}

/// Names a grammers participant role; unknown variants are reported as `unknown`.
pub(crate) fn role_name(role: &Role) -> &'static str {
    match role {
        Role::User(_) => "member",
        Role::Creator(_) => "creator",
        Role::Admin(_) => "admin",
        Role::Banned(_) => "banned",
        Role::Left(_) => "left",
        _ => "unknown",
    }
}

/// The role membership of one participant, as grammers' `ParticipantPermissions` answers it.
///
/// This is a different question from the rights a role carries: it says *what the member is* in
/// the chat, not what they are allowed to do, and only a channel participant can be an admin that
/// may add other admins. Every flag is a plain boolean because the layer answers all of them for
/// every variant.
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ParticipantPermissionsDto {
    /// True for the chat or channel creator.
    pub(crate) is_creator: bool,
    /// True for a creator too, because a creator has every right an admin has.
    pub(crate) is_admin: bool,
    /// True for a banned channel participant.
    pub(crate) is_banned: bool,
    /// True for a member that left the channel on their own.
    pub(crate) has_left: bool,
    /// True for a normal member with no restrictions and no admin rights.
    pub(crate) has_default_permissions: bool,
    /// True for a creator and for an admin whose rights let them add admins.
    pub(crate) can_add_admins: bool,
}

/// Projects grammers' `ParticipantPermissions`.
pub(crate) fn participant_permissions_dto(
    permissions: &ClientParticipantPermissions,
) -> ParticipantPermissionsDto {
    ParticipantPermissionsDto {
        is_creator: permissions.is_creator(),
        is_admin: permissions.is_admin(),
        is_banned: permissions.is_banned(),
        has_left: permissions.has_left(),
        has_default_permissions: permissions.has_default_permissions(),
        can_add_admins: permissions.can_add_admins(),
    }
}

#[cfg(test)]
mod tests {
    //! Wire-contract tests for the participant-permissions projection.

    use grammers_client::client::chats::ParticipantPermissions;
    use grammers_client::grammers_tl_types as tl;
    use serde::Serialize;
    use serde_json::json;

    use super::{participant_permissions_dto, ParticipantPermissionsDto};

    fn assert_json(dto: &impl Serialize, expected: serde_json::Value) {
        let text = serde_json::to_string(dto).expect("a projection always encodes");
        let actual: serde_json::Value = serde_json::from_str(&text).expect("the encoding is JSON");
        assert_eq!(actual, expected);
    }

    /// Admin rights with every flag cleared, which is what a test does not care about.
    fn no_rights() -> tl::enums::ChatAdminRights {
        tl::enums::ChatAdminRights::Rights(tl::types::ChatAdminRights {
            change_info: false,
            post_messages: false,
            edit_messages: false,
            delete_messages: false,
            ban_users: false,
            invite_users: false,
            pin_messages: false,
            add_admins: false,
            anonymous: false,
            manage_call: false,
            other: false,
            manage_topics: false,
            post_stories: false,
            edit_stories: false,
            delete_stories: false,
            manage_direct_messages: false,
        })
    }

    /// The document of a participant with no role at all, which each case below amends.
    fn empty() -> serde_json::Value {
        json!({
            "isCreator": false,
            "isAdmin": false,
            "isBanned": false,
            "hasLeft": false,
            "hasDefaultPermissions": false,
            "canAddAdmins": false,
        })
    }

    #[test]
    fn a_creator_is_an_admin_that_can_add_admins() {
        let permissions = ParticipantPermissions::Channel(tl::enums::ChannelParticipant::Creator(
            tl::types::ChannelParticipantCreator {
                user_id: 7,
                admin_rights: no_rights(),
                rank: None,
            }
            .into(),
        ));
        let mut expected = empty();
        expected["isCreator"] = json!(true);
        expected["isAdmin"] = json!(true);
        expected["canAddAdmins"] = json!(true);
        assert_json(&participant_permissions_dto(&permissions), expected);
    }

    #[test]
    fn an_admin_can_add_admins_only_when_its_rights_allow_it() {
        let mut rights = match no_rights() {
            tl::enums::ChatAdminRights::Rights(rights) => rights,
        };
        rights.add_admins = true;
        let permissions = ParticipantPermissions::Channel(tl::enums::ChannelParticipant::Admin(
            tl::types::ChannelParticipantAdmin {
                can_edit: true,
                is_self: false,
                user_id: 7,
                inviter_id: None,
                promoted_by: 8,
                date: 1_700_000_000,
                admin_rights: tl::enums::ChatAdminRights::Rights(rights),
                rank: None,
            }
            .into(),
        ));
        let mut expected = empty();
        expected["isAdmin"] = json!(true);
        expected["canAddAdmins"] = json!(true);
        assert_json(&participant_permissions_dto(&permissions), expected);
    }

    #[test]
    fn a_banned_member_has_left_and_a_departed_member_reports_it() {
        let banned = ParticipantPermissions::Channel(tl::enums::ChannelParticipant::Banned(
            tl::types::ChannelParticipantBanned {
                left: true,
                peer: tl::enums::Peer::User(tl::types::PeerUser { user_id: 7 }),
                kicked_by: 8,
                date: 1_700_000_000,
                banned_rights: tl::enums::ChatBannedRights::Rights(tl::types::ChatBannedRights {
                    view_messages: true,
                    send_messages: false,
                    send_media: false,
                    send_stickers: false,
                    send_gifs: false,
                    send_games: false,
                    send_inline: false,
                    embed_links: false,
                    send_polls: false,
                    change_info: false,
                    invite_users: false,
                    pin_messages: false,
                    manage_topics: false,
                    send_photos: false,
                    send_videos: false,
                    send_roundvideos: false,
                    send_audios: false,
                    send_voices: false,
                    send_docs: false,
                    send_plain: false,
                    until_date: 0,
                }),
            }
            .into(),
        ));
        let mut expected = empty();
        expected["isBanned"] = json!(true);
        assert_json(&participant_permissions_dto(&banned), expected);

        let left = ParticipantPermissions::Channel(tl::enums::ChannelParticipant::Left(
            tl::types::ChannelParticipantLeft {
                peer: tl::enums::Peer::User(tl::types::PeerUser { user_id: 7 }),
            }
            .into(),
        ));
        let mut expected = empty();
        expected["hasLeft"] = json!(true);
        assert_json(&participant_permissions_dto(&left), expected);
    }

    #[test]
    fn a_plain_channel_member_has_default_permissions() {
        let permissions =
            ParticipantPermissions::Channel(tl::enums::ChannelParticipant::Participant(
                tl::types::ChannelParticipant {
                    user_id: 7,
                    date: 1_700_000_000,
                    subscription_until_date: None,
                }
                .into(),
            ));
        let mut expected = empty();
        expected["hasDefaultPermissions"] = json!(true);
        assert_json(&participant_permissions_dto(&permissions), expected);
    }

    #[test]
    fn a_small_group_creator_and_admin_still_answer_the_flags() {
        let creator = ParticipantPermissions::Chat(tl::enums::ChatParticipant::Creator(
            tl::types::ChatParticipantCreator { user_id: 7 }.into(),
        ));
        let mut expected = empty();
        expected["isCreator"] = json!(true);
        expected["isAdmin"] = json!(true);
        expected["canAddAdmins"] = json!(true);
        assert_json(&participant_permissions_dto(&creator), expected);

        let admin = ParticipantPermissions::Chat(tl::enums::ChatParticipant::Admin(
            tl::types::ChatParticipantAdmin {
                user_id: 7,
                inviter_id: 8,
                date: 1_700_000_000,
            }
            .into(),
        ));
        let mut expected = empty();
        expected["isAdmin"] = json!(true);
        // A small-group admin carries the layer's full rights, but grammers only reports
        // `can_add_admins` for a channel participant.
        assert_json(&participant_permissions_dto(&admin), expected);
    }

    #[test]
    fn an_empty_projection_still_carries_every_flag() {
        let dto = ParticipantPermissionsDto {
            is_creator: false,
            is_admin: false,
            is_banned: false,
            has_left: false,
            has_default_permissions: false,
            can_add_admins: false,
        };
        assert_json(&dto, empty());
    }
}
