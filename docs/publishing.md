# Publishing to Maven Central

Releases use the Maven Central Publisher Portal through its OSSRH Staging API compatibility endpoint. The workflow is intentionally tag-driven: a push of a non-SNAPSHOT tag named `v<version>` builds the six bundled native libraries, packages the Kotlin JAR, signs every published artifact, uploads the staging repository, and asks the Portal to validate and publish it automatically.

## Coordinates

The published coordinates are:

```text
io.github.j0s3f:kotlogramme:<version>
```

Maven Central artifacts are immutable. Never reuse or retag a version after it has been submitted.

## One-time publisher setup

1. Sign in to the [Central Publisher Portal](https://central.sonatype.com) with the `J0s3f` GitHub account.
2. Verify the `io.github.j0s3f` namespace. GitHub-linked accounts are normally eligible for automatic verification of this namespace.
3. Create a Portal user token at <https://central.sonatype.com/usertoken>. Store its two displayed values; the password cannot be shown again after closing the dialog.
4. Create an OpenPGP signing key. Upload its public key to a public keyserver, and keep the ASCII-armored private key and passphrase private.
5. Add these repository secrets in GitHub under **Settings → Secrets and variables → Actions**:
   - `CENTRAL_USERNAME` — Portal token username
   - `CENTRAL_PASSWORD` — Portal token password
   - `SIGNING_KEY` — complete ASCII-armored private OpenPGP key, including its header and footer
   - `SIGNING_PASSWORD` — passphrase for that private key

The release workflow fails before uploading when any secret is absent. Never put a token, a private key, or its passphrase in Git, workflow files, issues, or chat.

## Releasing

Ensure `main` is green, choose a new semantic version, and create an annotated tag:

```bash
git checkout main
git pull --ff-only
git tag -a v0.2.0 -m "Release 0.2.0"
git push origin v0.2.0
```

The tag runs the full release: `raw-schema` and the five native builds, `package` (which bundles and
verifies the six native libraries), `publish-central` (uploads to the Central Portal with automatic
publication enabled) and `github-release` (attaches the per-platform native archives and the JAR to
a GitHub Release).

Inspect the `publish-central` log and the Portal deployment page. A first publication or a failed
validation may require intervention in the Portal; published versions cannot be changed or removed.

## Local dry run

To inspect generated publication metadata without credentials or signing keys:

```bash
./gradlew -Pversion=0.1.0 :kotlogramme-kotlin:generatePomFileForMavenJavaPublication
```

For a local signed publish, provide the four secrets as environment variables and use the Central publication task. Do not use a release version for an experiment: Central versions are immutable.

## JitPack

JitPack builds this repository because `v0.6.0` is a Git tag, but the artifact it serves is **not
usable at runtime**, so JitPack is not offered as an install option. Maven Central remains the
supported channel; its JAR is the one CI builds with all six native libraries bundled.

What was observed when the `v0.6.0` build was requested from `https://jitpack.io`:

- JitPack ran a plain Gradle build (`gradle publishToMavenLocal`) and reported `BUILD SUCCESSFUL in 24s`.
- It published exactly one module, under the coordinates **`com.github.J0s3f:kotlogram:v0.6.0`**. The
  group/artifact are derived from the repository name, not from `settings.gradle.kts` or the module
  name; `.../kotlogramme-kotlin/...` returns `404`. The POM names the project `kotlogramme` and depends
  only on `kotlin-stdlib` and `kotlinx-serialization-json-jvm`.
- The served JAR (`kotlogram-v0.6.0.jar`, about 1.95 MB) contains **no `native/` entries at all**. The
  build log shows why: `file or directory '/home/jitpack/build/generated/native', not found`. The
  native libraries are produced outside Gradle by cargo in GitHub Actions and copied in by
  `tasks.processResources`; JitPack never runs that step.
- A scratch consumer that resolved `com.github.J0s3f:kotlogram:v0.6.0` from the JitPack repository
  compiled against the real API (`com.github.badoualy.telegram.api.Kotlogram.API_LAYER` is `229`), but
  constructing a client failed at runtime with
  `java.lang.UnsatisfiedLinkError: no kotlogramme in java.library.path`. `NativeLibraryLoader` falls
  back to `System.loadLibrary("kotlogramme")` when the bundled resource is absent.

A `jitpack.yml` could add a Rust toolchain and run the cargo build before Gradle, but that is not a
general fix:

- JitPack builds on Linux x86_64 only, so at most one of the six platform libraries
  (linux-x86_64) could be bundled; Windows, macOS and ARM64 consumers would still fail.
- The native crate's `grammers-*` dependencies are git dependencies on codeberg, which the build would
  have to fetch and compile from source inside JitPack, subject to its availability and build timeout.
- JitPack reads `jitpack.yml` from the commit of the tag being built, so a file added to `main` after
  `v0.6.0` cannot retrofit that tag; only future tags could benefit.

Publishing through Maven Central avoids all of this, because the tag-driven GitHub Actions workflow
builds all six native libraries and bundles them into the released JAR before it is uploaded.
