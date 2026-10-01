# Merge gates

Work that is finished but deliberately not merged, and what unblocks it.

## `chore/actions-node24` - move every action onto Node 24

**State: blocked, waiting on a real JitPack release.**

GitHub removed Node 20 from Actions runners on 23 September 2026
([deprecation post](https://github.blog/changelog/2025-09-19-deprecation-of-node-20-on-github-actions-runners/),
[removal post](https://github.blog/changelog/2026-09-23-node-20-is-no-longer-available-in-github-actions/)).
The branch bumps every pinned action to a version that runs on Node 24.

It is **not** merged yet, on purpose. The gate is: **JitPack must actually serve a release first.** The
reason is that the Node 24 job touches the same workflows that publish, and the JitPack packaging in
`jitpack.yml` has been through several failures already (`VERSION` carrying the tag, JitPack's curl
predating `--fail-with-body`, `AFTER_INSTALL_CMD`). Bumping the action majors on top of unproven
packaging would make a future failure ambiguous: it would not be clear whether the release broke or the
bump did.

So the order is:

1. `v0.9.2` completes its release run.
2. JitPack serves `0.9.2` - verified by resolving the POM and the jar at
   `https://jitpack.io/com/github/J0s3f/kotlogram/v0.9.2/`, with six native entries in the jar.
3. Only then merge `chore/actions-node24`, and watch the next run to confirm the bumped actions work.

### Why the gate matters here specifically

`actions/upload-artifact` and `actions/download-artifact` are several majors behind (v4 against v7 and
v8). Those two carry the jar and the six native archives between jobs and into the GitHub Release, so a
wrong bump breaks the release **silently** rather than loudly - the release would still be created, just
with missing or misplaced assets. That is the failure mode the gate exists to keep separable from a
packaging failure.

### When it is merged

The next tag should be watched rather than assumed. Specifically check that:

- every expected asset appears on the GitHub Release: `kotlogramme-<version>.jar`, its `.asc` and
  `.asc.pub`, and the six `kotlogramme-native-*.zip` archives;
- `publish-central` reports its outcome in the step summary;
- `publish-jitpack` resolves the version it polls for.

## Pre-rewrite refs

The pre-rewrite tags (`0.0.1`-`0.0.6`, `1.0.0-RC1`-`RC3`) and the branches `develop`, `imgbot` and
`master` were deleted from GitHub. They sorted above the `v0.x` series, so JitPack would have presented
a 2017 artifact as the newest version. Their tips are archived locally under `refs/archive/tags/*` and
`refs/archive/branches/*`; recover with `git branch <name> refs/archive/branches/<name>`.

## JitPack: the release race, and why the tag matters

A tag push triggers two things at once: the GitHub Actions run that **creates** the release, and
JitPack's own build of that tag. JitPack can therefore ask for
`releases/download/v<version>/kotlogramme-<version>.jar` before the release exists, get a 404, and fail
the build. That is what happened to `v0.9.2`.

`jitpack.yml` now waits for the asset (up to ~10 minutes) instead of failing on the first 404.
**But `jitpack.yml` is read from the tagged commit**, so:

- a retry added to `main` after a tag does **not** apply to that tag;
- `v0.9.2` is tagged at `95be894c`, which predates the retry. Its build only works because its release
  already exists by now - the first `curl` succeeds. The wait loop is for tags cut from `b76653da` or
  later.

This is the practical rule: **a packaging fix only takes effect on the next tag.** When changing
`jitpack.yml`, expect the current release to need a rebuild rather than a retag, and check that the
release its build reads from is already published before asking JitPack for it.

### Rate limiting

JitPack answers `429` per repository and version when the same artifact path is requested repeatedly.
It is not IP-based - a different address hits the same limit. Polling a build log or an artifact URL in a
loop will trip it, and a `429` says nothing about the build's state. Wait, and request once.

### Rebuilding

A failed or deleted build can be rebuilt by signing in at <https://jitpack.io/#J0s3f/kotlogram> and
requesting the version again. Deleting the build in that UI is what allows a fresh one; without it the
cached result is served. The GitHub Release is untouched either way.

