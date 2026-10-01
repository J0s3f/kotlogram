# Release 0.9.0 doc audit

Working checklist for the 0.9.0 release. Every claim below was read out of the file it names, not
inferred. Tick each row as it is reconciled; the "actual" column is the value measured on the release
candidate, so it must be re-measured if anything else lands before the tag.

## Wrong claims to fix

| File | Line | Says | Actual |
| --- | --- | --- | --- |
| `README.md` | 12 | "105 native operations, each reachable from Kotlin through a **278-test** Rust suite" | Rust is **289**; JVM is **296** |
| `README.md` | 75 | "The current release is **0.8.0**" | 0.9.0 |
| `README.md` | 85 | `implementation("io.github.j0s3f:kotlogramme:0.8.0")` | 0.9.0 |
| `README.md` | 93 | `implementation 'io.github.j0s3f:kotlogramme:0.8.0'` | 0.9.0 |
| `README.md` | 103 | `<version>0.8.0</version>` | 0.9.0 |
| `docs/compatibility.md` | 139 | "Not mapped yet: ... **chatlist folder creation**" | creation IS mapped via `messagesUpdateDialogFilter`; only `chatlists.*` sharing was missing |
| `docs/integration-testing.md` | all | describes only `LiveTelegramIntegrationTest` | a second live class `LiveUpdatePollIntegrityTest` exists and is not mentioned |
| `docs/gap-closure-plan.md` | 3, 134 | presents G1-G3 as pending work for 0.9.0 | G1-G4 are all merged; the plan needs a status line |
| `docs/publishing.md` | — | verify the version/tag instructions match the actual release flow | not yet checked |

## To update when the chatlist work lands

- `README.md` line 28 (features list): mention folder sharing beside dialog filters.
- `docs/compatibility.md`: the `chatlists.*` bullet.
- `CHANGELOG.md`: the `## Unreleased` heading gains the chatlist entry, then gets dated for 0.9.0.
- Operation count in `README.md` line 12 and anywhere else it appears: 105 to whatever it becomes.

## Claims to re-verify, not assume

These are stated in prose and could silently drift. Check each against the tree before the tag:

- `README.md` L47: "supports **Telegram TL layer 229**" and the pinned rev
  `42d4b51059524cf7c8c9a07745a1071057fa1526` - confirm `native/Cargo.toml` still pins exactly this.
- `Kotlogram.API_LAYER` reports the same value - confirm in source.
- `docs/ci-costs.md`: the cache claim was corrected once already; confirm nothing else regressed.
- Test counts in `docs/gap-closure-roadmap.md` and `grammers-parity-plan.md` if they state any.

## Things that must NOT be claimed

- Do not claim the story or paid-media surfaces are mapped; they are still behind `invokeRaw`.
- Do not claim live verification of the chatlist operations unless it is actually run. The
  orchestrator owns live testing; a task that has not done it must not imply it.
- Do not call G4 (the code generator) done. Its first deliverable is a design decision, and it is not
  in 0.9.0.
- Keep the honest limits: `upload_stream` unknown length, `--no-detect` reading back as `[video]`,
  WebM/VP9 metadata loss being Telegram's behaviour.

## Release mechanics

- `CHANGELOG.md`: replace `## Unreleased` with the dated version heading; do not leave both.
- Version comes from `-Pversion`, defaulting to `0.1.0-SNAPSHOT`; the tag drives publication, so the
  tag and the CHANGELOG heading must agree.
- GitHub Release must carry the plain bundled jar with all natives, as every prior release did.
- After publishing, confirm the version actually appears on Maven Central before the client bumps to
  it - the last release needed ~18 minutes and a premature bump would not resolve.
