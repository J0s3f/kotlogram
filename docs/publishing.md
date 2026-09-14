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
git tag -a v0.1.0 -m "Release 0.1.0"
git push origin v0.1.0
```

The `publish-central` job then uploads to the Central Portal with automatic publication enabled. Inspect its GitHub Actions log and the Portal deployment page. A first publication or a failed validation may require intervention in the Portal; published versions cannot be changed or removed.

## Local dry run

To inspect generated publication metadata without credentials or signing keys:

```bash
./gradlew -Pversion=0.1.0 :kotlogramme-kotlin:generatePomFileForMavenJavaPublication
```

For a local signed publish, provide the four secrets as environment variables and use the Central publication task. Do not use a release version for an experiment: Central versions are immutable.
