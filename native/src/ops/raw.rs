//! Typed wrappers around raw Telegram methods that grammers does not surface, such as contacts and account management.
//!
//! Placeholder: no operation is routed here yet. A follow-up task adds its names to
//! [`OPERATIONS`] and their handlers to [`route`], without touching any other module.

use super::Handler;

/// Operation names routed by this module.
pub(crate) const OPERATIONS: &[&str] = &[];

/// Routes [operation] to its handler, or returns `None` when the name is not this module's.
pub(crate) fn route(_operation: &str) -> Option<Handler> {
    None
}
