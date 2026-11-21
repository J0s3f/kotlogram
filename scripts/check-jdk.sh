#!/usr/bin/env bash
#
# Fails when the JDK on PATH is a build affected by Java4Shell (CVE-2026-114938), an out-of-bounds
# heap write in HotSpot's AVX-512 Base64 decoder that affects OpenJDK 17.0.6 and later, 21, 25 and 27.
#
#   scripts/check-jdk.sh
#
# The fixed builds (Oracle and Temurin, November 2026) are 17.0.21.1, 21.0.13.1, 25.0.5.1 and
# 27.0.1.1. setup-java resolves `17` to the newest Temurin 17, which is already fixed, so this only
# trips when a runner image serves a stale cached build. A major version that is not listed here has
# no fix and is rejected when it is in the affected range.
set -euo pipefail

raw="$(java -version 2>&1 | head -n1 | sed -E 's/.*"([^"]+)".*/\1/')"
# Drop a build or vendor suffix such as `+2` or `-LTS`.
version="${raw%%[+-]*}"
major="${version%%.*}"

case "${major}" in
  17) minimum="17.0.21.1" ;;
  21) minimum="21.0.13.1" ;;
  25) minimum="25.0.5.1" ;;
  27) minimum="27.0.1.1" ;;
  *)
    if [ "${major}" -ge 18 ] 2>/dev/null; then
      echo "JDK ${raw} is a line without a Java4Shell fix; use 17, 21, 25 or 27." >&2
      exit 1
    fi
    echo "JDK ${raw} is outside the affected range."
    exit 0
    ;;
esac

# `sort -V` puts the older version first; 17.0.21.0.3 (vulnerable) sorts before 17.0.21.1 (fixed).
oldest="$(printf '%s\n%s\n' "${minimum}" "${version}" | sort -V | head -n1)"
if [ "${oldest}" != "${minimum}" ]; then
  echo "JDK ${raw} is older than ${minimum} and is affected by Java4Shell (CVE-2026-114938)." >&2
  exit 1
fi
echo "JDK ${raw} includes the Java4Shell fix (>= ${minimum})."
