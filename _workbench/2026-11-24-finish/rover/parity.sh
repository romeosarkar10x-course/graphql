#!/usr/bin/env bash
# Compose every course supergraph.yaml with Rover 1.0.1 (ELv2 accepted) and with the course composer
# (The Guild's), then compare the two supergraphs with directives' argument order and blank lines normalised.
source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
export APOLLO_ELV2_LICENSE=accept APOLLO_TELEMETRY_DISABLED=true APOLLO_HOME=/course/.cache/fed/rover
R=/course/.cache/fed/bin/rover; OUT=/course/_workbench/2026-11-24-finish/rover; cd /course
norm() { node -e '
const {parse, print, Kind} = require(process.env.NODE_PATH + "/graphql");
const d = parse(require("fs").readFileSync(process.argv[1], "utf8"));
const defs = d.definitions.map(x => print(x)).sort();
console.log(defs.join("\n\n"));' "$1"; }
export NODE_PATH=/course/modules/m15-federation/contract/compose/node_modules
for y in $(find modules -name "supergraph*.yaml" -not -path "*/target/*" | sort); do
  n=$(echo "$y" | tr / _ | sed 's/.yaml$//'); d=$(dirname "$y")
  (cd "$d" && $R supergraph compose --config "$(basename "$y")" --output "$OUT/$n.rover.graphql" > "$OUT/$n.rover.log" 2>&1) && r=ok || r=FAIL
  (cd "$d" && compose "$(basename "$y")" "$OUT/$n.guild.graphql" > "$OUT/$n.guild.log" 2>&1) && g=ok || g=FAIL
  diffs=-
  if [[ $r == ok && $g == ok ]]; then
    norm "$OUT/$n.rover.graphql" > "$OUT/$n.rover.norm"; norm "$OUT/$n.guild.graphql" > "$OUT/$n.guild.norm"
    diffs=$(diff "$OUT/$n.rover.norm" "$OUT/$n.guild.norm" | grep -c '^[<>]')
  fi
  echo "$y rover=$r guild=$g differing-lines=$diffs"
done
