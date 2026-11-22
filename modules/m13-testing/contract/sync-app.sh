#!/usr/bin/env bash
# Copy the contract apps (shelfie-app, and dgs-app for lab 13.2) (pom.xml, src/main) into every lab that ships a copy of it, so the copies never drift.
# Run from anywhere after changing contract/shelfie-app: modules/m13-testing/contract/sync-app.sh
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; app="$here/shelfie-app"
for d in "$here"/../labs/{01-unit-tests,02-graphql-tests,08-pit-tournament}/{starter,solution}; do
  [[ -d "$d" ]] || continue
  rm -rf "$d/src/main"; mkdir -p "$d/src"; cp -r "$app/src/main" "$d/src/main"; cp "$app/pom.xml" "$d/pom.xml"
  echo "synced $(realpath --relative-to="$here/.." "$d")"
done
for d in "$here"/../labs/02-graphql-tests/{starter,solution}/dgs; do
  rm -rf "$d/src/main"; cp -r "$here/dgs-app/src/main" "$d/src/main"; cp "$here/dgs-app/pom.xml" "$d/pom.xml"
done
