#!/usr/bin/env bash
# Capstone stage 2: a working API. Build, run, serve the contract, pass the Hurl suite, behave on the core probes.
source /course/tools/testlib.sh
source /course/modules/m19-capstone/contract/capstone.sh
cap_manifest || finish
cap_api_schema || finish
cap_build || finish
cap_up || finish
echo "  -- contract first"
probe_served_schema
echo "  -- the conformance suite"
cap_hurl
echo "  -- core behaviour"
probe_pagination
probe_typed_error
probe_idempotent
finish
