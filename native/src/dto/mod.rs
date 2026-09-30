//! JSON result shapes, one file per entity, plus the projections shared across domains.
//!
//! Every module below owns a struct and the constructor that fills it, so an operation never
//! builds a projection by hand. [message_dto] and [user_dto] are re-exported here because several
//! domains return the same entities; the rest are reached through their own module.

pub(crate) mod account;
pub(crate) mod action;
pub(crate) mod auth;
pub(crate) mod contacts;
pub(crate) mod dialog;
pub(crate) mod dialog_meta;
pub(crate) mod files;
pub(crate) mod folders;
pub(crate) mod inline;
pub(crate) mod markup;
pub(crate) mod media;
pub(crate) mod message;
pub(crate) mod participant;
pub(crate) mod peer;
pub(crate) mod permissions;
pub(crate) mod stickers;
pub(crate) mod update;
pub(crate) mod user;

#[cfg(test)]
mod tests;

pub(crate) use message::message_dto;
pub(crate) use user::user_dto;
