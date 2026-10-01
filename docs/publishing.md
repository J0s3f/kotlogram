# Publishing

The library is published to two channels. **JitPack is the recommended one** because it has no
publishing limits and always serves the latest release; **Maven Central is a fallback**, because its
monthly limits can refuse a release for reasons unrelated to the release being correct.

| Channel | Coordinates | Notes |
| --- | --- | --- |
| JitPack (recommended) | `com.github.J0s3f:kotlogram:<version>` | Needs `maven("https://jitpack.io")` |
| Maven Central (fallback) | `io.github.j0s3f:kotlogramme:<version>` | Signed; subject to monthly limits |

Releases are tag-driven. Pushing a non-SNAPSHOT tag named `v<version>` builds the six bundled native
libraries, packages the Kotlin JAR, signs the artifacts, creates a GitHub Release, publishes the signed
JAR to JitPack, and - best effort - uploads to the Central Portal.

## Why Central is the fallback

Maven Central introduced monthly publishing limits on 1 October 2026. The authoritative page is
[central.sonatype.org/publish/maven-central-publishing-limits](https://central.sonatype.org/publish/maven-central-publishing-limits);
the enforcement-timing change is announced at
[community.sonatype.com](https://community.sonatype.com/t/update-maven-central-publishing-limits-enforcement-moved-to-october-1/16475).

Central tracks three monthly metrics per organization. The one that affects this project is **release
size**, whose free threshold sits at the 90th percentile: **78 MB per calendar month**, counting every
published file - the jar, its `.asc` signatures and checksums, the sources jar and the javadoc jar.

The bundled jar alone is **32.4 MB**, so a release lands in the same order of magnitude as the monthly
threshold once signatures, checksums, sources and javadoc are counted, and a second release in one month
is exactly the pattern the limits target. An over-limit month gets a 30-day grace period; after that,
further releases that would keep the organization over the metric are refused until usage drops, an
exemption is approved, or a paid option is in place. Exemptions can be requested from
`central-support@sonatype.com`.

None of this is a statement about the software - the artifact is unchanged whichever channel serves it.
It is the reason Central can no longer be the channel the README points at first.

## Coordinates

```text
JitPack        com.github.J0s3f:kotlogram:<version>
Maven Central  io.github.j0s3f:kotlogramme:<version>
```

JitPack derives its group and artifact from the **repository name**, not from `settings.gradle.kts` or
the module name: the repository is `kotlogram`, so the artifact is `kotlogram` even though the published
name is `kotlogramme`. `com.github.J0s3f:kotlogramme` does not exist, and neither does a
`kotlogramme-kotlin` sub-coordinate.

Artifacts on both channels are **immutable**. Never reuse or retag a version after it has been published.

## One-time publisher setup

### Maven Central

1. Sign in to the [Central Publisher Portal](https://central.sonatype.com) with the `J0s3f` GitHub account.
2. Verify the `io.github.j0s3f` namespace. GitHub-linked accounts are normally eligible for automatic verification of this namespace.
3. Create a Portal user token at <https://central.sonatype.com/usertoken>. Store its two displayed values; the password cannot be shown again after closing the dialog.
4. Create an OpenPGP signing key. Upload its public key to a public keyserver, and keep the ASCII-armored private key and passphrase private.
5. Add these repository secrets in GitHub under **Settings → Secrets and variables → Actions**:
   - `CENTRAL_USERNAME` — Portal token username
   - `CENTRAL_PASSWORD` — Portal token password
   - `SIGNING_KEY` — complete ASCII-armored private OpenPGP key, including its header and footer
   - `SIGNING_PASSWORD` — passphrase for that private key

The signing key is needed for both channels: it signs the Central publication, and it produces the
detached signature that ships with the GitHub Release for JitPack consumers to verify.

Never put a token, a private key, or its passphrase in Git, workflow files, issues, or chat.

### JitPack

No account, token or secret is needed for a public repository. JitPack builds a version the first time
it is requested; see [jitpack.io/#J0s3f/kotlogram](https://jitpack.io/#J0s3f/kotlogram) to look it up or
trigger a build. A build takes up to 15 minutes, so the first consumer to ask for a new version may wait
- raise Gradle's HTTP timeouts if that bites:

```properties
systemProp.org.gradle.internal.http.connectionTimeout=180000
systemProp.org.gradle.internal.http.socketTimeout=180000
```

The release workflow does not rely on a consumer triggering the build: `publish-jitpack` requests the new
version itself and waits for it, so a broken `jitpack.yml` fails the release rather than a consumer's
first resolve.

#### On webhooks

There is a JitPack webhook (`https://jitpack.io/api/webhooks`) and this repository has one configured,
but it is **not** what gets a release built. JitPack's own documentation is explicit on both counts:

- Ahead-of-time builds for releases are JitPack's own scheduler: "JitPack periodically checks for new
  releases and builds them ahead-of-time." Nothing has to be configured for that.
- The webhook is for branches: "The webhook will trigger a build for branches that you have previously
  used with JitPack."

Observed on the `v0.9.1` push: the branch delivery returned `200` and the tag delivery returned `404`,
which matches the documented branch-only behaviour. The hook is therefore harmless but not load-bearing,
and the release would still be published without it.

## JitPack packaging

`jitpack.yml` at the repository root overrides JitPack's default "build from source" behaviour with
"download the CI-built jar and install it". JitPack's own FAQ points at this technique for publishing an
existing jar: [Publish an existing jar file to jitpack](https://gist.github.com/jitpack-io/f928a858aa5da08ad9d9662f982da983).

It matters because a source build on JitPack cannot produce a usable artifact here:

- JitPack builds on **Linux x86_64 only**, so a source build could bundle at most one of the six native
  libraries; Windows, macOS and ARM64 consumers would fail at runtime with `UnsatisfiedLinkError`.
- The native crate depends on grammers through **git dependencies on codeberg**, which a source build
  would have to fetch and compile inside JitPack's build timeout.
- JitPack never runs cargo, so `generated/native` does not exist and the packaged jar carries no
  `native/` entries at all.

Downloading the jar CI already built removes all three obstacles: nothing is compiled, every native
library is present because CI bundled them, and the bytes are identical to the Central artifact.

This was observed directly on the `v0.6.0` build, before `jitpack.yml` existed: JitPack reported
`BUILD SUCCESSFUL`, served `com.github.J0s3f:kotlogram:v0.6.0`, and the 1.95 MB jar contained no
`native/` entries - its build log said `file or directory '/home/jitpack/build/generated/native', not
found`. A scratch consumer compiled against the real API but failed at runtime with
`java.lang.UnsatisfiedLinkError: no kotlogramme in java.library.path`. That is the failure mode
`jitpack.yml` exists to avoid, and it is worth re-checking after any change to it.

## Signing

JitPack does not sign artifacts **for** you - its FAQ notes it provides checksums but no signing - so the
signature is produced by this pipeline instead. The artifact is signed either way; what differs from
Maven Central is only where the signature is delivered:

- Gradle signs the publication whenever `SIGNING_KEY` and `SIGNING_PASSWORD` are present.
- The detached ASCII-armored signature is attached to the GitHub Release as
  `kotlogramme-<version>.jar.asc` beside the jar, with the public key as `kotlogramme-<version>.jar.asc`'s
  partner, `SIGNING_KEY.pub.asc`, so the key can be fetched without leaving the release.

The delivery point matters and is worth stating rather than glossing: JitPack's repository exposes the
jar, the POM and JitPack's own md5/sha checksums, and nothing this repository controls can add an `.asc`
there. A consumer resolving from JitPack therefore cannot have the signature verified automatically
during dependency resolution, the way a Central consumer can; they fetch the `.asc` from the GitHub
Release and check it by hand. That is the one respect in which the JitPack channel is weaker than
Central, and it is a property of JitPack's artifact set rather than of the artifact.

```bash
gpg --import SIGNING_KEY.pub.asc
gpg --verify kotlogramme-<version>.jar.asc kotlogramme-<version>.jar
```

## Releasing

Ensure `main` is green, choose a new semantic version, and create an annotated tag:

```bash
git checkout main
git pull --ff-only
git tag -a v0.10.0 -m "Release 0.10.0"
git push origin v0.10.0
```

The tag runs, in order:

1. `raw-schema` and the six native builds.
2. `package`, which bundles and verifies the native libraries into the jar.
3. `publish-central` - **best effort**. It uploads to the Central Portal with automatic publication
   enabled, and a rejection is reported without failing the run, because a publishing limit must not
   cost the release.
4. `github-release`, which attaches the per-platform native archives, the jar and its signature.
5. `publish-jitpack`, which points JitPack at the release created in step 4.

Steps 4 and 5 run whether or not step 3 succeeded. The release is therefore never lost to a Central
limit; when Central refuses, the release is still on JitPack and on GitHub.

Inspect the `publish-central` step output for the outcome. A first publication or a failed validation
may require intervention in the Portal; published versions cannot be changed or removed.

## Local dry run

To inspect generated publication metadata without credentials or signing keys:

```bash
./gradlew -Pversion=0.1.0 :kotlogramme-kotlin:generatePomFileForMavenJavaPublication
```

For a local signed publish, provide the four secrets as environment variables and use the Central
publication task. Do not use a release version for an experiment: versions on both channels are
immutable.
