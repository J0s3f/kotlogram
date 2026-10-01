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
