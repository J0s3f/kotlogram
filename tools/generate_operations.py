#!/usr/bin/env python3
"""Regenerate `native/operations.txt` from the Rust dispatcher.

The inventory is the shared contract between the native crate and the Kotlin bridge: the Rust
unit tests in `native/src/ops/tests.rs` assert it equals `all_operations()` plus the names the JNI
exports implement, and `OperationParityTest` asserts it equals the `@Operation` names in Kotlin.
Both fail when the file drifts.

Editing that file by hand is the one thing every feature task would otherwise have to do the same
way, so run this instead after adding an operation:

    python tools/generate_operations.py

It reads the `OPERATIONS` arrays out of `native/src/ops/*.rs` and the exported names out of
`native/src/lib.rs`, then writes the union, sorted, with a trailing newline.
"""

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
OPS = REPO / "native" / "src" / "ops"
LIB = REPO / "native" / "src" / "lib.rs"
INVENTORY = REPO / "native" / "operations.txt"

# `pub(crate) const OPERATIONS: &[&str] = &[ ... ];`
OPERATIONS_BLOCK = re.compile(
    r"pub\(crate\)\s+const\s+OPERATIONS[^=]*=\s*&\[(?P<body>.*?)\];",
    re.DOTALL,
)
# The export is `fn Java_org_kotlogramme_TelegramClient_00024Native_<name>(`.
JNI_EXPORT = re.compile(r"fn\s+Java_org_kotlogramme_TelegramClient_00024Native_(?P<name>\w+)\s*\(")
STRING_LITERAL = re.compile(r'"([^"]*)"')

# The JNI exports that are the transport rather than an operation. They open and close the session
# and carry a dispatched request, but none of them is dispatched by name, so none of them appears
# in Kotlin as an `@Operation`. The Rust gate lists the rest as `JNI_OPERATIONS`.
TRANSPORT_EXPORTS = frozenset({"create", "close", "request"})


def read_operations() -> set[str]:
    """Every name declared by an `OPERATIONS` array, across all domain modules."""
    names: set[str] = set()
    for module in sorted(OPS.glob("*.rs")):
        for block in OPERATIONS_BLOCK.finditer(module.read_text(encoding="utf-8")):
            names.update(STRING_LITERAL.findall(block.group("body")))
    return names


def read_jni_operations() -> set[str]:
    """Every name a JNI export implements as an operation instead of routing through dispatch."""
    source = LIB.read_text(encoding="utf-8")
    exported = {match.group("name") for match in JNI_EXPORT.finditer(source)}
    return exported - TRANSPORT_EXPORTS


def main() -> int:
    names = read_operations() | read_jni_operations()
    if not names:
        print("no operation names found; refusing to write an empty inventory", file=sys.stderr)
        return 1
    INVENTORY.write_text("".join(f"{name}\n" for name in sorted(names)), encoding="utf-8")
    print(f"wrote {len(names)} operations to {INVENTORY.relative_to(REPO)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
