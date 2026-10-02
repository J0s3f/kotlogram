#!/usr/bin/env bash
#
# Keeps the version the documentation advertises in step with the release.
#
#   scripts/version.sh set   0.9.12   # rewrite the docs to 0.9.12
#   scripts/version.sh check 0.9.12   # fail when the docs name any other version
#
# The release version is the tag; nothing else can be the source of truth. This exists because the
# files a reader trusts had drifted: the README still advertised 0.9.5 when 0.9.11 was the release,
# and the JitPack fallback pointed at the same old version.
#
# Only the two files that describe the *current* release are touched. `CHANGELOG.md` and `docs/*.md`
# are deliberately excluded: their version numbers are a record of what shipped when, and rewriting
# them would falsify history. A release is prepared by running `set`, and CI runs `check` on the tag
# so a stale README cannot ship.
set -euo pipefail

mode="${1:?usage: scripts/version.sh <set|check> <version>}"
version="${2:?usage: scripts/version.sh <set|check> <version>}"

case "${version}" in
  v*) version="${version#v}" ;;
esac

# The files whose version means "the current release".
files=(README.md jitpack.yml)

# The version family the docs carry, e.g. 0.9 for 0.9.12. Everything in this family is "current".
family="$(printf '%s' "${version}" | sed -E 's/\.[0-9]+$//')"

family_pattern="$(printf '%s' "${family}" | sed -E 's/\./\\./g')\.[0-9]+"

case "${mode}" in
  set)
    for file in "${files[@]}"; do
      sed -i -E "s/${family_pattern}/${version}/g" "${file}"
    done
    echo "docs now advertise ${version}"
    ;;

  check)
    status=0
    if ! grep -q "${version}" README.md; then
      echo "README.md does not name the release ${version}" >&2
      status=1
    fi
    for file in "${files[@]}"; do
      stale="$(grep -oE "${family_pattern}" "${file}" | grep -vx "${version}" | sort -u | tr '\n' ' ' || true)"
      if [ -n "${stale}" ]; then
        echo "${file}: names ${stale}- but the release is ${version}" >&2
        status=1
      fi
    done
    if [ "${status}" -eq 0 ]; then
      echo "docs agree with ${version}"
    fi
    exit "${status}"
    ;;

  *)
    echo "unknown mode '${mode}'; use set or check" >&2
    exit 2
    ;;
esac
