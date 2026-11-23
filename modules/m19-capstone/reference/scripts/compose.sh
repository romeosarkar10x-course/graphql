#!/usr/bin/env bash
# Compose router/supergraph.graphql from the two contract schemas, with the course's composer (The Guild's MIT
# library, Module 15). CI runs it and fails if the committed supergraph differs.
set -euo pipefail
cd "$(dirname "$0")/.."
source /course/tools/testlib.sh
source /course/modules/m15-federation/contract/fed.sh
trap - EXIT
compose router/supergraph.yaml router/supergraph.graphql
echo "router/supergraph.graphql composed"
