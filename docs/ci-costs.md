# CI costs and limits

Decision note on whether to gate the native build to release tags. Verified against GitHub's
current documentation (fetched 2026-10-01) and this account's `gh`-visible state.

## Verdict

**Cost is not a reason to gate builds to tags — both repositories are public, and standard
GitHub-hosted runners are free and unlimited on public repositories.** That includes Linux, Windows,
macOS, and the arm64 labels the library matrix uses. The tag-only change is still defensible for
*non-cost* reasons (queueing, hammering codeberg, check noise), but it should not be sold as a cost
measure, and a lightweight `main` build should come back.

## 1. Verified numbers

GitHub-hosted runner minutes are free and unlimited for **standard** runners in **public**
repositories; private repositories draw on a plan quota instead.

| Fact | Value | Source |
| --- | --- | --- |
| Standard runners free/unlimited on public repos | "Use of the standard GitHub-hosted runners is free and unlimited on public repositories." | https://docs.github.com/en/actions/reference/runners/github-hosted-runners |
| Same, workflow-runner page | "Use of the standard GitHub-hosted runners is free and unlimited on public repositories." | https://docs.github.com/en/actions/how-tos/write-workflows/choose-where-workflows-run/choose-the-runner-for-a-job |
| Public usage is free; private draws a quota | "GitHub Actions usage is **free** … for **public repositories** that use standard GitHub-hosted runners. For **private repositories**, each account receives a quota of free minutes, artifact storage, and cache storage." | https://docs.github.com/en/billing/concepts/product-billing/github-actions |
| GitHub Free included amounts (private) | 2,000 minutes/month; 500 MB artifact storage; 10 GB cache per repository | https://docs.github.com/en/billing/concepts/product-billing/github-actions (also https://docs.github.com/en/actions/reference/limits) |
| Per-minute rates (private / larger) | Linux 2-core $0.006; Linux arm64 $0.005; Windows 2-core $0.010; macOS $0.062 | https://docs.github.com/en/billing/reference/actions-runner-pricing |
| Larger runners always charged | "The larger runners are not free for public repositories." | https://docs.github.com/en/billing/reference/actions-runner-pricing |
| Cache size limit | 10 GB per repository default; entries unused for 7 days evicted; configurable per repo | https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching |
| Cache retention configurable | default 7 days; up to 90 days public / 365 days private | https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository |
| Artifact/run retention default | 90 days; configurable 1–90 days public, 1–400 days private. From 2026-10-01 retention also covers checks, workflow runs and commit statuses. | https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository |
| Job concurrency (Free plan) | 20 total concurrent jobs; max 5 concurrent macOS jobs | https://docs.github.com/en/actions/reference/limits |
| Job/run limits | 6 h per job (GitHub-hosted); 35 days per workflow run; 256 jobs per matrix | https://docs.github.com/en/actions/reference/limits |

### Q1 — does "free and unlimited" cover all standard runners, or a subset?

It covers the runners in the **standard GitHub-hosted runners** tables, which span Linux (x64 and
arm64, including `ubuntu-slim`), Windows (x64 and arm64) and macOS. It excludes **larger runners**
and **custom images**, which are always billed even on public repositories. Neither repository uses
a larger runner or a runner group.

### Q2 — are `ubuntu-24.04-arm` and `windows-11-arm` standard runners, or preview/larger?

Both are **standard** GitHub-hosted runners:

- Both labels are listed in the *public* standard-runner table with no "Public preview" marker (only
  `xcode-27` carries one): https://docs.github.com/en/actions/reference/runners/github-hosted-runners
- GitHub's Jan 2026 changelog: "Linux and Windows arm64 standard GitHub-hosted runners are now
  supported … in all repositories", four vCPUs in public repos, "free-tier eligible":
  https://github.blog/changelog/2026-01-29-arm64-standard-runners-are-now-available-in-private-repositories/
- The `windows-11-arm` image README banners GA ("now generally available in GitHub Actions"):
  https://github.com/actions/runner-images/blob/main/images/windows/Windows11-Arm64-Readme.md

So for this project's matrix, `ubuntu-24.04-arm` and `windows-11-arm` are standard arm64 runners and
are **free in a public repository**. There is no ARM preview caveat left that affects cost.

### Q3 — does the Windows/macOS multiplier apply on public repos?

No. The OS differences appear only in the private/larger pricing: the flat SKUs are Linux 2-core
$0.006/min, Windows 2-core $0.010/min, macOS $0.062/min (roughly 1.7x and 10x the Linux rate).
Because standard-runner usage in a public repository is free and unlimited, no minute is metered and
no multiplier is applied. GitHub's own usage-metrics page also notes metrics "do not apply minute
multipliers", i.e. multipliers are a private-repo allowance concept:
https://docs.github.com/en/actions/reference/usage-limits-billing-and-administration

### Q4 — what still binds on a public repository

- **Artifact/run retention**: 90 days by default; configurable 1–90 days on public repos. Since
  2026-10-01 this also governs checks, workflow runs and commit statuses.
- **Cache**: 10 GB per repository default, caches unused for 7 days are evicted; retention is
  configurable (up to 90 days on public repos). Cache scoping is by branch/tag — a run cannot reuse a
  cache from a *different* tag (`release-a` cannot read `release-b`'s cache), which is the cause of
  the read-only-cache warning noted in the build logs.
- **Concurrency**: Free plan allows 20 concurrent jobs total, of which at most 5 may be macOS. The
  library matrix launches 5 native jobs plus `raw-schema` at once, so it is under the totals, but
  macOS is the scarce resource.
- **Job limits**: 6 h per job, 35 days per run, 256 jobs per matrix.
- The 500 MB artifact / 2,000 minute entitlements in the Free-plan table are the **private**
  repository allowance; public standard-runner usage does not consume them.

## 2. This account's actual position

From `gh` (token scopes: `gist`, `read:org`, `repo`, `workflow`):

- `gh api /users/J0s3f` — user `J0s3f`, id 604089, 76 public repos. **No plan field is returned.**
- `gh api /user/plan` → **HTTP 404**.
- `gh api /users/J0s3f/settings/billing/actions` → **HTTP 404**, with GitHub's message that the
  endpoint "needs the user scope". The current token lacks that scope, so **Actions billing/usage is
  not readable**; this is a finding, not a zero.
- `gh repo view J0s3f/kotlogram` and `J0s3f/kotlogramme-cli` → both `visibility: PUBLIC`,
  `isPrivate: false`, owner `J0s3f`, default branch `main`.

Measured job minutes from `gh run list` + the run's jobs API (jobs with a start and end time,
excluding 5 impossible ~24 h cancelled macOS jobs from 2026-09-14 — a queued/stuck artifact, since
GitHub-hosted jobs are capped at 6 h). Window covered: 2026-09-14 → 2026-09-30.

| Repo | Runs | Jobs | Linux | Windows | macOS | Total wall min |
| --- | --- | --- | --- | --- | --- | --- |
| `J0s3f/kotlogram` (public) | 67 | 488 | 258.7 | 365.2 | 238.8 | **862.8** |
| `J0s3f/kotlogramme-cli` (private until today) | 60 | 170 | 72.6 | 112.6 | 34.3 | **219.5** |

Combined that is ~1,082 job-minutes over roughly two weeks.

The library was already public, so its 863 minutes were never metered. The client was private, so its
219.5 minutes were metered — but even with the classic Windows 2x / macOS 10x allowance accounting
(72.6 + 2×112.6 + 10×34.3 = **640.8 billed minutes**) it stayed well under the Free plan's 2,000.

Illustrative: had the *library* matrix run private on every push, the same two weeks would have
counted **258.7 + 2×365.2 + 10×238.8 = 3,377 billed minutes** — over the 2,000/month allowance. At
the published private SKUs those two weeks would be about **US$20** (macOS alone ~$14.8). That is the
number that would have justified gating — and it no longer applies to a public repo.

## 3. Cost vs the other reasons to limit builds

Cost (now moot):

- Public + standard runners ⇒ free and unlimited, all five matrix labels included. No multiplier,
  no allowance, no storage charge for the runs themselves.

Non-cost reasons (these are real, and are the honest justification for the tag-only change):

- **Runner queueing.** macOS is capped at 5 concurrent and is the scarcest pool; the five ~24 h
  cancelled macOS jobs and the cancelled runs in the history show the matrix can sit waiting. A full
  six-job matrix on every docs commit competes with itself for runners.
- **Hammering a flaky dependency.** `native` fetches `grammers` from codeberg as a git dependency.
  Running the matrix on every push multiplies load on a third-party host the project does not
  control; tag-only reduced that.
- **Commit-status noise.** Five native checks plus package/publish per push clutters PRs and `main`.
- **Cache warming is the counter-argument.** As the current `build.yml` comment notes, "nothing runs
  on main any more, so a tag build has no warmed cache to read". The Rust cache on a tag is read-only
  by default (tag scoping), so every release rebuilds cold. A `main` job would keep the default-branch
  cache warm.

## 4. Recommendation

1. **Do not describe the tag-only change as a cost saving — it is not.** Both repositories are
   public; standard runners, ARM included, are free and unlimited. Cost is not a reason to keep
   `main` from building.
2. **Restore a `main` build, but a lighter one.** Keep the full five-label native matrix + package +
   publish on `v*` tags (correct as is). Add a `push`/`pull_request` job on `main` that builds and
   tests on Linux only (the JVM `package` path plus one native target). That is effectively free and
   removes the queueing, codeberg-hammering and status-noise arguments for blocking all builds.
3. **Optionally run the full matrix on PRs to `main` or nightly**, not on every push. It is free, so
   the only cost is runner contention; a PR/nightly cadence buys platform coverage without the noise.
4. **Warm the cache from `main`.** A trusted `push` job on the default branch can populate caches that
   later runs restore; tag runs cannot share each other's caches.
5. **Leave `kotlogramme-cli` as it is.** Its three-OS `ci.yml` on every push/PR is now free, and its
   measured footprint (219.5 min over two weeks, ~641 metered minutes even under multiplier
   accounting) never approached the Free allowance.
