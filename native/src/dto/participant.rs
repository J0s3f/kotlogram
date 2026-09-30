//! Chat-participant projection.

use grammers_client::client::ParticipantPermissions as ClientParticipantPermissions;
use grammers_client::peer::{Participant as ClientParticipant, Role};
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
            dto.date = Some(role.date().as_millisecond());
            dto.invited_by = role.inviter_id().and_then(|id| id.bare_id());
        }
        Role::Creator(role) => {
            dto.permissions = Some(role.permissions().into());
            dto.rank = role.rank().map(ToOwned::to_owned);
        }
        Role::Admin(role) => {
            dto.can_edit = Some(role.can_edit());
            dto.invited_by = role.inviter_id().and_then(|id| id.bare_id());
            dto.promoted_by = role.promoted_by().and_then(|id| id.bare_id());
            dto.date = Some(role.date().as_millisecond());
            dto.permissions = Some(role.permissions().into());
            dto.rank = role.rank().map(ToOwned::to_owned);
        }
        Role::Banned(role) => {
            dto.left = Some(role.left());
            dto.kicked_by = Some(role.kicked_by().bare_id().unwrap_or(0));
            dto.date = Some(role.date().as_millisecond());
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
