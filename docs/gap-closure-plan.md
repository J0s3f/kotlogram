# Closing the four remaining gaps

Plan of record for the four limits [`compatibility.md`](compatibility.md) lists under "Not mapped yet",
and which the README repeats. Successor to the completed
[`gap-closure-roadmap.md`](gap-closure-roadmap.md) (T0-T9, all merged). Baseline at the time of
writing: 101 operations, 257 Rust tests, 266 JVM tests, released as 0.8.0.

## What each task actually needs, and why it is feasible

All four were checked against the pinned grammers checkout before being scheduled, because three of
them are "grammers has no surface for this" and one is not.

### G1 - The formatting entities on a reply's quoted text

grammers already carries them. `Message::reply_header()` returns
`Option<tl::enums::MessageReplyHeader>` straight off the raw message, and `MessageReplyHeader` holds
the quoted text's entities. The projection in `native/src/dto/message.rs` already extracts the
message's *own* entities through `fmt_entities()` and explicitly documents `quote_entities` as absent
(line 160). This is a projection gap, not a protocol gap: add the quoted text and its entities beside
the existing `text`/`entities`/`html_text`/`markdown_text` fields.

Deliver: `MessageQuoteDto` (text, entities, html, markdown) on the message DTO; Kotlin projection and
the documented getter; client rendering of the quoted line.

### G2 - Sticker install / uninstall, and per-peer notification settings

grammers has **no** sticker or notification surface of its own, but the generated TL layer has
everything, which is the same position the existing five sticker operations are in. Confirmed in the
build output `grammers-tl-types-*/out/generated_functions.rs` and `generated_enums.rs`:

| TL function | struct | namespace |
| --- | --- | --- |
| `messages.installStickerSet` | `InstallStickerSet` | `tl::functions::messages` |
| `messages.uninstallStickerSet` | `UninstallStickerSet` | `tl::functions::messages` |
| `account.getNotifySettings` | `GetNotifySettings` | `tl::functions::account` |
| `account.updateNotifySettings` | `UpdateNotifySettings` | `tl::functions::account` |

Supporting types: `InputStickerSet` and `StickerSetInstallResult` are in `tl::enums`;
`PeerNotifySettings`, `InputPeerNotifySettings`, `NotifyPeer`, `InputNotifyPeer` are present.

Copy the precedent already in `native/src/ops/stickers.rs`: build one `tl::functions::*` struct and
call `native.client.invoke(&...)` under `runtime.block_on`, then project. `input_sticker_set` is
already a helper there and takes short name / id / access hash.

Deliver: install, uninstall, get and set notification settings as routed operations; DTOs for the
notify settings and the install result; Kotlin facade methods; client commands.

### G3 - A background `UpdateCallback` loop

`UpdatesApi.dispatchNextUpdate` already waits for one update and hands it to the callback. The missing
piece is a thread that calls it repeatedly, plus a way to stop it and to observe failures. This is
pure Kotlin in `UpdatesApi.kt` and needs no native work and no new operation.

The design constraint that matters: a loop that cannot be stopped, or that swallows an exception, is
worse than no loop. It needs an explicit lifecycle (start / stop / isRunning), a clean shutdown that
does not block on the 30 s poll, and a decision about where an exception goes.

### G4 - The manifest-driven Kotlin code generator

The largest by an order of magnitude, and the only one where "feasible" needs a caveat.
`kotlogramme-kotlin/src/main/resources/raw/telegram-layer-229.json` is 850 KB and already carries
everything a generator needs: 813 functions and 1658 constructors, each function with its
`constructorId`, `declaration`, `parameters` and `result` strings. `tools/generate_raw_schema.py`
already produces it from grammers' `api.tl` and CI already gates the committed copy with `cmp`.

So the input side is done and the deployment gate exists. What is missing is the generator itself plus
the design decision it forces: **which subset gets typed**. Generating all 813 functions as Kotlin
request/response classes is a large amount of code to maintain against a layer that changes, and the
facade deliberately stops at 101 curated operations. A generated surface of 813 would undercut the
curated design rather than extend it.

Recommendation: **generate the codec layer, not the facade**. That is, generate the TL types and
encode/decode for the constructors, and keep the named operation surface curated by hand. This is
what `invokeRaw` already does dynamically at runtime; making it generated buys compile-time safety
and removes a runtime parse, without a second public API competing with the facade.

This task is scheduled **last**, and its first deliverable is a written design decision, not code. If
the design does not settle cleanly, it stops there and the gap stays open behind `invokeRaw`, which
already works.

## Order and why

G1 and G2 are independent and can run in parallel - they touch different DTOs, different `ops/`
modules and different operation names. G3 is independent of both and touches only Kotlin. G4 depends
on nothing but must not start until 0.9.0 is released, because the generated code changes the
published artifact's shape.

```
G1 (quoted entities)  ─┐
G2 (stickers + notify) ─┼─> release 0.9.0 ─> G3 (update loop) ─> release 0.10.0 ─> G4 (generator)
G3 could ship earlier  ─┘
```

Recommended: G1 + G2 + G3 together as 0.9.0 (they are three small, independent changes and one release
cycle costs a full five-platform matrix), then G4 alone on top.

## What each task must not do

- Do not change the pinned grammers rev in `native/Cargo.toml`, and do not run `cargo update`.
- Do not touch `.github/**` or `docs/publishing.md` from a task branch.
- Do not widen the curated operation surface opportunistically. If a task notices another missing
  operation, it records it and stops.
- Every added operation name goes in `native/operations.txt`; the Rust and JVM parity tests fail by
  design if that is missed.

## Verification per task

- Rust: `cargo test --manifest-path native/Cargo.toml`, and every pre-existing test stays green.
- JVM: `cargo build --release`, stage the DLL into `generated/native/windows-x86_64/`, then
  `.\gradlew.bat --no-daemon test` with the pinned JDK 17/21 installation paths. The JVM suite loads
  the native library, so this step is not optional.
- Offline tests must cover the new projections and the facade; a test that needs a live session does
  not belong in the suite.
- **Live** verification is the orchestrator's, not the task's, and is recorded in
  `docs/live-test-findings.md` on the client side. No task claims live verification it did not do.

## Client follow-ups

Each gap has a client-visible surface and is worth carrying through:

| Gap | Client surface |
| --- | --- |
| G1 | the quoted line under a reply in the message table, with its entities styled |
| G2 | commands to install/uninstall a sticker set and to read/set notification settings for a peer |
| G3 | an `on-update` / `watch` command that streams updates through the background loop |
| G4 | none - it changes no behaviour a CLI user sees |

The client's port/gateway chain is `application/port/api/*` to `adapter/telegram/*`; each of these
needs a port, a gateway implementation and an offline fake, or the client's own test suite cannot
cover it.

## Per-task release notes

- **0.9.0** - G1, G2, G3. Adds quoted-reply entities, sticker install/uninstall, notification
  settings, and a background update loop.
- **0.10.0** - G4, only if its design decision settles. Otherwise 0.10.0 is skipped and the gap
  remains documented.