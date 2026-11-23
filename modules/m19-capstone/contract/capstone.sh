# The capstone checker's runtime half: source it after tools/testlib.sh, with LAB_DIR set to the capstone repo.
# Everything it does is driven by the repo's capstone.json (contract/MANIFEST.md describes every key).
#
#   cap_manifest            capstone.json parses and has the keys every stage needs
#   cap_api_schema          compose the contract schemas into /tmp/cap/api.graphql (the schema clients see)
#   cap_build               run "build"
#   cap_up / cap_down       start every "run" process and wait until each "ready" URL answers / stop them all
#   cap_token <role> [arg]  a course token for that role ("tokens" in the manifest), e.g. cap_token member --expired
#   cap_send <op> [vars] [role] [token-arg]   send an operation from operations/ to "api", the way the manifest
#                           says ("send": documentId or query); prints the body; st prints its HTTP status
#   probe_*                 the checks: pagination, typed_error, idempotent, auth, trusted, expensive, n1,
#                           realtime, client, served_schema, trace, dashboard (each prints pass/fail lines)
CAP=/course/modules/m19-capstone/contract
CAP_TMP=/tmp/cap
CAP_PIDS=()
mkdir -p "$CAP_TMP"
source /course/modules/m15-federation/contract/fed.sh
trap - EXIT
trap 'cap_down' EXIT

m() { jq -r "$1 // empty" "$LAB_DIR/capstone.json" 2>/dev/null; }   # a manifest value as text ("" when absent)
mj() { jq -c "$1 // null" "$LAB_DIR/capstone.json" 2>/dev/null; }    # a manifest value as JSON
capcheck() { java -cp "$(lib_cp)" "$CAP/Capcheck.java" "$@"; }

cap_manifest() {
  if [[ ! -f $LAB_DIR/capstone.json ]]; then fail "capstone.json is missing" "copy the template from the starter (bin/course start m19/01)"; return 1; fi
  if ! jq -e . "$LAB_DIR/capstone.json" >/dev/null 2>&1; then fail "capstone.json is not valid JSON"; return 1; fi
  local missing=() k
  for k in product subgraphs operations hurl; do [[ $(mj ".$k") == null ]] && missing+=("$k"); done
  [[ -z $(m .product) ]] && missing+=(product)
  [[ $(m '.subgraphs | length') -ge 1 ]] || missing+=("subgraphs (at least one)")
  if (( ${#missing[@]} )); then fail "capstone.json: fill in ${missing[*]}" "modules/m19-capstone/contract/MANIFEST.md lists every key"; return 1; fi
  pass "capstone.json: $(m .product), $(m '.subgraphs | length') subgraph(s)"
}

# The composed API schema. One subgraph is composed too: the composer accepts a plain schema as a subgraph.
cap_api_schema() {
  local n i name file
  n=$(m '.subgraphs | length')
  { echo "subgraphs:"
    for ((i = 0; i < n; i++)); do
      name=$(m ".subgraphs[$i].name"); file=$(m ".subgraphs[$i].schema")
      echo "  $name:"; echo "    routing_url: $(m ".subgraphs[$i].url // \"http://localhost:400$((i + 1))/graphql\"")"
      echo "    schema:"; echo "      file: $LAB_DIR/$file"
    done; } > "$CAP_TMP/supergraph.yaml"
  for ((i = 0; i < n; i++)); do
    file=$(m ".subgraphs[$i].schema")
    [[ -s $LAB_DIR/$file ]] || { fail "subgraph schema $file is missing"; return 1; }
  done
  if compose "$CAP_TMP/supergraph.yaml" "$CAP_TMP/supergraph.graphql" "$CAP_TMP/api.graphql" 2> "$CAP_TMP/compose.err"; then
    pass "the contract composes ($n subgraph schema(s))"
  else
    fail "the contract does not compose" "$(head -5 "$CAP_TMP/compose.err")"; return 1
  fi
}

cap_build() {
  local cmd; cmd=$(m .build)
  [[ -n $cmd ]] || { fail "capstone.json: \"build\" is empty"; return 1; }
  if (cd "$LAB_DIR" && timeout 1200 bash -c "$cmd") > "$CAP_TMP/build.log" 2>&1; then pass "build: $cmd"
  else fail "build failed: $cmd" "$(grep -m5 -E 'ERROR|error:' "$CAP_TMP/build.log" | cut -c1-200)"; return 1; fi
}

# Each process runs in its own session, so stopping it stops its children too (a script that starts a JVM).
cap_up() {
  local n i name cmd ready t
  n=$(m '.run | length'); [[ $n -gt 0 ]] || { fail "capstone.json: \"run\" lists no processes"; return 1; }
  for ((i = 0; i < n; i++)); do
    name=$(m ".run[$i].name"); cmd=$(m ".run[$i].cmd"); ready=$(m ".run[$i].ready")
    (cd "$LAB_DIR" && exec setsid bash -c "$cmd") > "$CAP_TMP/run-$name.log" 2>&1 &
    CAP_PIDS+=($!)
    for t in $(seq 1 240); do curl -fs -o /dev/null -m 2 "$ready" && break; kill -0 "${CAP_PIDS[-1]}" 2>/dev/null || break; sleep 0.5; done
    if curl -fs -o /dev/null -m 2 "$ready"; then pass "$name is up ($ready)"
    else fail "$name did not get ready at $ready" "$(tail -3 "$CAP_TMP/run-$name.log" | cut -c1-200)"; return 1; fi
  done
}

cap_down() {
  local p
  for p in "${CAP_PIDS[@]}"; do kill -- -"$p" 2>/dev/null || kill "$p" 2>/dev/null; done
  for p in "${CAP_PIDS[@]}"; do wait "$p" 2>/dev/null; done
  CAP_PIDS=()
  declare -F ops_stop >/dev/null && ops_stop
  return 0
}

cap_token() { # role [extra token args]
  local args; args=$(m ".tokens.\"$1\" | join(\" \")")
  [[ -n $args ]] || { echo "course: capstone.json has no token for role $1" >&2; return 1; }
  java /course/modules/m10-security/contract/Token.java $args "${@:2}"
}

# {{run}} in variables becomes a fresh value per checker run, so idempotency keys and names do not collide.
CAP_RUN="capcheck-$(date +%s)-$RANDOM"
cap_vars() { local v="${1:-null}"; [[ $v == null || -z $v ]] && v='{}'; echo "${v//\{\{run\}\}/$CAP_RUN}"; }

cap_body() { # op vars -> the request body, by the manifest's send mode
  local op=$1 vars; vars=$(cap_vars "$2")
  if [[ $(m '.send // "query"') == documentId ]]; then
    local id; id=$(capcheck id "$LAB_DIR/$(m .trusted.manifest)" "$op") || return 1
    jq -nc --arg id "$id" --arg op "$op" --argjson v "$vars" '{documentId: $id, operationName: $op, variables: $v}'
  else
    jq -nc --arg q "$(capcheck doc "$LAB_DIR/$(m .operations)" "$op")" --arg op "$op" --argjson v "$vars" '{query: $q, operationName: $op, variables: $v}'
  fi
}

cap_send() { # op [vars] [role] [token-arg] [extra curl args]
  local op=$1 vars=${2:-'{}'} role=${3:-} targ=${4:-} h=() body
  if [[ -n $role && $role != null ]]; then h=(-H "Authorization: Bearer $(cap_token "$role" $targ)"); fi
  body=$(cap_body "$op" "$vars") || { echo 000 > "$CAP_TMP/status"; echo '{}'; return 1; }
  curl -s -m 20 -o "$CAP_TMP/resp.json" -w '%{http_code}' "$(m .api)" -H 'Content-Type: application/json' \
    -H 'Accept: application/graphql-response+json, application/json' "${h[@]}" "${@:5}" --data-binary "$body" > "$CAP_TMP/status"
  cat "$CAP_TMP/resp.json"
}
st() { cat "$CAP_TMP/status" 2>/dev/null; }   # the HTTP status of the last cap_send (it runs in a subshell)

has_errors() { jq -e '(.errors // []) | length > 0' >/dev/null 2>&1; }

# --- probes ---------------------------------------------------------------------------------------------------

probe_pagination() {
  local p; p=$(mj .probes.pagination); [[ $p != null ]] || { fail "probes.pagination is missing"; return; }
  local op path size fv av vars r1 r2 cursor
  op=$(jq -r .operation <<<"$p"); path=$(jq -r .connection <<<"$p"); size=$(jq -r '.pageSize // 2' <<<"$p")
  fv=$(jq -r '.firstVar // "first"' <<<"$p"); av=$(jq -r '.afterVar // "after"' <<<"$p")
  vars=$(jq -c --arg f "$fv" --argjson n "$size" '(.variables // {}) + {($f): $n}' <<<"$p")
  r1=$(cap_send "$op" "$vars" "$(jq -r '.as // empty' <<<"$p")")
  cursor=$(jq -r ".data$path.pageInfo.endCursor // empty" <<<"$r1")
  local n1 more; n1=$(jq ".data$path.edges | length" <<<"$r1" 2>/dev/null); more=$(jq -r ".data$path.pageInfo.hasNextPage" <<<"$r1" 2>/dev/null)
  if [[ $n1 != "$size" || $more != true || -z $cursor ]]; then
    fail "pagination: page 1 of $op should have $size edges, hasNextPage and an endCursor" "$(cut -c1-200 <<<"$r1")"; return
  fi
  r2=$(cap_send "$op" "$(jq -c --arg a "$av" --arg c "$cursor" '. + {($a): $c}' <<<"$vars")" "$(jq -r '.as // empty' <<<"$p")")
  local ids1 ids2; ids1=$(jq -c "[.data$path.edges[].node.id]" <<<"$r1"); ids2=$(jq -c "[.data$path.edges[].node.id]" <<<"$r2")
  if [[ $(jq ". | length" <<<"$ids2" 2>/dev/null) -ge 1 ]] && jq -e --argjson a "$ids1" '(. - (. - $a)) | length == 0' <<<"$ids2" >/dev/null; then
    pass "pagination: $op pages by cursor without overlap ($ids1 then $ids2)"
  else fail "pagination: page 2 (after the cursor) is empty or repeats page 1" "page 1 $ids1, page 2 $ids2"; fi
}

probe_typed_error() {
  local p; p=$(mj .probes.typedError); [[ $p != null ]] || { fail "probes.typedError is missing"; return; }
  local op want r; op=$(jq -r .operation <<<"$p"); want=$(jq -r .expect <<<"$p")
  r=$(cap_send "$op" "$(jq -c '.variables // {}' <<<"$p")" "$(jq -r '.as // empty' <<<"$p")")
  if ! has_errors <<<"$r" && jq -e --arg t "$want" '[.. | objects | select(.__typename? == $t)] | length > 0' <<<"$r" >/dev/null 2>&1; then
    pass "typed error: $op answers $want in data, with no errors"
  else fail "typed error: $op should answer __typename $want in data, without errors" "$(cut -c1-250 <<<"$r")"; fi
}

probe_idempotent() {
  local p; p=$(mj .probes.idempotent); [[ $p != null ]] || { fail "probes.idempotent is missing"; return; }
  local op vars as c cop cvars cas cpath c0 c1 r1 r2
  op=$(jq -r .operation <<<"$p"); vars=$(jq -c '.variables // {}' <<<"$p"); as=$(jq -r '.as // empty' <<<"$p")
  c=$(jq -c .count <<<"$p"); cop=$(jq -r .operation <<<"$c"); cvars=$(jq -c '.variables // {}' <<<"$c")
  cas=$(jq -r '.as // empty' <<<"$c"); cpath=$(jq -r .path <<<"$c")
  count() { cap_send "$cop" "$cvars" "$cas" | jq "(.data$cpath) as \$x | if (\$x | type) == \"array\" then (\$x | length) else \$x end"; }
  c0=$(count); r1=$(cap_send "$op" "$vars" "$as"); r2=$(cap_send "$op" "$vars" "$as"); c1=$(count)
  if has_errors <<<"$r1"; then fail "idempotency: $op failed" "$(cut -c1-250 <<<"$r1")"; return; fi
  if [[ $(jq -cS .data <<<"$r1") == "$(jq -cS .data <<<"$r2")" ]]; then pass "idempotency: the repeated $op got the same answer"
  else fail "idempotency: the repeated $op got a different answer" "first $(jq -c .data <<<"$r1" | cut -c1-150), then $(jq -c .data <<<"$r2" | cut -c1-150)"; fi
  if [[ $c0 =~ ^[0-9]+$ && $c1 =~ ^[0-9]+$ ]] && (( c1 == c0 + 1 )); then pass "idempotency: $cop counted $c0 then $c1: one effect for two requests"
  else fail "idempotency: $cop should count one more after two identical requests" "before $c0, after $c1"; fi
}

probe_auth() {
  local n i c name op exp r denied allowed=0 refused=0
  n=$(m '.probes.auth | length'); (( n >= 4 )) || { fail "probes.auth: at least 4 cases (found ${n:-0})" "signed out, the wrong member, another role, a bad token"; return; }
  for ((i = 0; i < n; i++)); do
    c=$(mj ".probes.auth[$i]"); name=$(jq -r '.name // "case"' <<<"$c"); op=$(jq -r .operation <<<"$c"); exp=$(jq -r .expect <<<"$c")
    r=$(cap_send "$op" "$(jq -c '.variables // {}' <<<"$c")" "$(jq -r '.as // empty' <<<"$c")" "$(jq -r '.token // empty' <<<"$c")")
    denied=0; [[ $(st) == 401 || $(st) == 403 ]] && denied=1; has_errors <<<"$r" && denied=1
    if [[ $exp == denied && $denied == 1 ]]; then pass "auth: $name: refused"; refused=$((refused + 1))
    elif [[ $exp == allowed && $denied == 0 && $(st) == 200 ]]; then pass "auth: $name: allowed"; allowed=$((allowed + 1))
    else fail "auth: $name: expected $exp" "HTTP $(st) $(cut -c1-200 <<<"$r")"; fi
  done
  (( allowed >= 1 && refused >= 2 )) || fail "probes.auth: include at least one allowed and two refused cases"
}

probe_trusted() {
  local r
  r=$(curl -s -m 10 "$(m .api)" -H 'Content-Type: application/json' --data '{"query":"query CapcheckProbe { __typename }"}')
  if jq -e '.data.__typename? // empty' <<<"$r" >/dev/null 2>&1; then fail "trusted documents: an unknown document ran" "$(cut -c1-200 <<<"$r")"
  else pass "trusted documents: a document that is not in the manifest is refused"; fi
  if [[ $(m '.send // "query"') == documentId ]]; then
    local missing=() op
    for op in $(capcheck ops "$CAP_TMP/api.graphql" "$LAB_DIR/$(m .operations)" | jq -r '.operations[].name'); do
      capcheck id "$LAB_DIR/$(m .trusted.manifest)" "$op" >/dev/null 2>&1 || missing+=("$op")
    done
    (( ${#missing[@]} == 0 )) && pass "trusted documents: every operation has an id in $(m .trusted.manifest)" \
      || fail "trusted documents: no id for ${missing[*]}" "regenerate the manifest"
  else fail "trusted documents: capstone.json \"send\" should be documentId (clients send ids, not documents)"; fi
}

probe_expensive() {
  local p r; p=$(mj .probes.expensive); [[ $p != null ]] || { fail "probes.expensive is missing"; return; }
  r=$(cap_send "$(jq -r .operation <<<"$p")" "$(jq -c '.variables // {}' <<<"$p")" "$(jq -r '.as // empty' <<<"$p")")
  if has_errors <<<"$r" && jq -e '(.data // null) == null' <<<"$r" >/dev/null 2>&1; then
    pass "demand control: $(jq -r .operation <<<"$p") with $(jq -c '.variables' <<<"$p") is refused before it runs"
  else fail "demand control: the expensive request should be refused (errors, no data)" "HTTP $(st) $(cut -c1-200 <<<"$r")"; fi
}

cap_counter_sum() { # the sum of every counter in the probe
  local p=$1 n i url path total=0 v
  n=$(jq '.counters | length' <<<"$p")
  for ((i = 0; i < n; i++)); do
    url=$(jq -r ".counters[$i].url" <<<"$p"); path=$(jq -r ".counters[$i].path" <<<"$p")
    v=$(curl -fs -m 5 "$url" | jq "$path // 0" 2>/dev/null); total=$(jq -n "$total + ${v:-0}")
  done
  echo "$total"
}

probe_n1() {
  local p; p=$(mj .probes.n1); [[ $p != null ]] || { fail "probes.n1 is missing"; return; }
  local op as a b c d small large
  op=$(jq -r .operation <<<"$p"); as=$(jq -r '.as // empty' <<<"$p")
  cap_send "$op" "$(jq -c .small <<<"$p")" "$as" >/dev/null   # warm-up: lazy caches and first-request work
  a=$(cap_counter_sum "$p"); cap_send "$op" "$(jq -c .small <<<"$p")" "$as" >/dev/null; b=$(cap_counter_sum "$p")
  c=$(cap_counter_sum "$p"); cap_send "$op" "$(jq -c .large <<<"$p")" "$as" > "$CAP_TMP/n1.json"; d=$(cap_counter_sum "$p")
  small=$(jq -n "$b - $a"); large=$(jq -n "$d - $c")
  if has_errors < "$CAP_TMP/n1.json"; then fail "N+1: the large request failed" "$(cut -c1-200 "$CAP_TMP/n1.json")"; return; fi
  if jq -e -n "$small > 0 and $large <= $small + 2" >/dev/null; then
    pass "N+1: $op runs $small statements for $(jq -c .small <<<"$p") and $large for $(jq -c .large <<<"$p")"
  else fail "N+1: statements grow with the page" "small $small, large $large (counters: $(jq -c '[.counters[].url]' <<<"$p"))"; fi
}

probe_realtime() {
  local p; p=$(mj .probes.realtime); [[ $p != null ]] || { fail "probes.realtime is missing"; return; }
  local kind op t; kind=$(jq -r '.kind // "subscription"' <<<"$p"); op=$(jq -r .operation <<<"$p")
  if [[ $kind == defer ]]; then
    local r; r=$(cap_send "$op" "$(jq -c '.variables // {}' <<<"$p")" "$(jq -r '.as // empty' <<<"$p")" "" -H 'Accept: multipart/mixed')
    if grep -q -E '"(incremental|hasNext)"' <<<"$r"; then pass "@defer: $op answers in more than one part"
    else fail "@defer: $op should answer with incremental parts (multipart/mixed)" "$(cut -c1-200 <<<"$r")"; fi
    return
  fi
  local body url; body=$(cap_body "$op" "$(jq -c '.variables // {}' <<<"$p")"); url=$(jq -r ".url // \"$(m .api)\"" <<<"$p")
  curl -s -N -m 12 "$url" -H 'Content-Type: application/json' -H 'Accept: text/event-stream' --data-binary "$body" > "$CAP_TMP/sse.txt" 2>/dev/null &
  local cp=$!
  sleep 2
  t=$(jq -c .trigger <<<"$p")
  cap_send "$(jq -r .operation <<<"$t")" "$(jq -c '.variables // {}' <<<"$t")" "$(jq -r '.as // empty' <<<"$t")" > "$CAP_TMP/trigger.json"
  for _ in $(seq 1 16); do grep -q '^data:.*"data"' "$CAP_TMP/sse.txt" && break; sleep 0.5; done
  kill $cp 2>/dev/null; wait $cp 2>/dev/null
  if grep -q '^data:.*"data"' "$CAP_TMP/sse.txt"; then pass "real time: $op delivered an event after $(jq -r .operation <<<"$t") ($(grep -m1 '^data:' "$CAP_TMP/sse.txt" | cut -c1-120))"
  else fail "real time: no event on $op within 8 s of $(jq -r .operation <<<"$t")" "stream: $(head -c 200 "$CAP_TMP/sse.txt"); trigger: $(head -c 150 "$CAP_TMP/trigger.json")"; fi
}

probe_client() {
  local cmd out env=() k v; cmd=$(m .client.cmd)
  [[ -n $cmd ]] || { fail "capstone.json: client.cmd is missing (the Java client)"; return; }
  for k in $(m '.client.env // {} | keys[]'); do
    v=$(m ".client.env.\"$k\""); [[ $v =~ ^\{\{token:([a-z]+)\}\}$ ]] && v=$(cap_token "${BASH_REMATCH[1]}"); env+=("$k=$v")
  done
  if out=$(cd "$LAB_DIR" && env "${env[@]}" timeout 120 bash -c "$cmd" 2>&1) && [[ -n $out ]]; then
    pass "the Java client runs: $cmd ($(head -1 <<<"$out" | cut -c1-80))"
  else fail "the Java client failed: $cmd" "$(tail -3 <<<"$out" | cut -c1-200)"; fi
}

# Each subgraph serves the schema in the contract: _service { sdl } (federated) or introspection (plain).
probe_served_schema() {
  local n i name url sdl
  n=$(m '.subgraphs | length')
  { echo "subgraphs:"; for ((i = 0; i < n; i++)); do
      name=$(m ".subgraphs[$i].name"); url=$(m ".subgraphs[$i].url")
      sdl=$(curl -s -m 10 "$url" -H 'Content-Type: application/json' --data '{"query":"{ _service { sdl } }"}' | jq -r '.data._service.sdl // empty')
      if [[ -z $sdl ]]; then   # not a federated subgraph: introspection (a single service, the shorter tracks)
        jq -nc --rawfile q "$CAP/introspection.graphql" '{query: $q}' | curl -s -m 10 "$url" -H 'Content-Type: application/json' --data-binary @- > "$CAP_TMP/introspection-$name.json"
        sdl=$(capcheck printsdl "$CAP_TMP/introspection-$name.json" 2>/dev/null)
      fi
      [[ -n $sdl ]] || { fail "$name at $url answers neither _service { sdl } nor introspection"; return; }
      echo "$sdl" > "$CAP_TMP/served-$name.graphql"
      echo "  $name:"; echo "    routing_url: $url"; echo "    schema:"; echo "      file: $CAP_TMP/served-$name.graphql"
    done; } > "$CAP_TMP/served.yaml"
  compose "$CAP_TMP/served.yaml" "$CAP_TMP/served-supergraph.graphql" "$CAP_TMP/served-api.graphql" 2> "$CAP_TMP/served.err" \
    || { fail "the served subgraph schemas do not compose" "$(head -3 "$CAP_TMP/served.err")"; return; }
  if diff <(capcheck coords "$CAP_TMP/api.graphql") <(capcheck coords "$CAP_TMP/served-api.graphql") > "$CAP_TMP/coords.diff"; then
    pass "contract first: the running subgraphs serve exactly the contract's schema ($(capcheck coords "$CAP_TMP/api.graphql" | wc -l) coordinates)"
  else fail "the running schema differs from the contract (< contract, > served)" "$(head -6 "$CAP_TMP/coords.diff" | tr '\n' ' ')"; fi
}

cap_hurl() {
  local file vf args=() role out
  file=$(m .hurl.file); vf=$(m .hurl.variablesFile)
  [[ -s $LAB_DIR/$file ]] || { fail "the Hurl suite $file is missing"; return; }
  [[ -n $vf ]] && args+=(--variables-file "$LAB_DIR/$vf")
  for role in $(m '.tokens // {} | keys[]'); do args+=(--variable "token_$role=$(cap_token "$role")"); done
  args+=(--variable "token_expired=$(cap_token "$(m '.tokens | keys[0]')" --expired)" --variable "run=$CAP_RUN" --variable "api=$(m .api)")
  if out=$(cd "$LAB_DIR" && hurl --test "${args[@]}" "$file" 2>&1); then
    pass "hurl $file: $(grep -oE 'Executed requests: [0-9]+' <<<"$out" | grep -oE '[0-9]+') requests green"
  else fail "hurl $file" "$(grep -vE '^\s*$' <<<"$out" | head -14 | tr '\n' ' ' | cut -c1-600)"; fi
}

# --- stage 4: the observability stack --------------------------------------------------------------------------

# Tempo and Grafana from Module 16; Prometheus with the manifest's scrape targets ("metrics").
cap_ops_start() {
  source /course/modules/m16-production/contract/ops.sh
  trap 'cap_down' EXIT
  local targets; targets=$(m '[.metrics[]? | capture("^https?://(?<h>[^/]+)(?<p>/.*)$")] | group_by(.p) | map({path: .[0].p, hosts: map(.h)})')
  [[ $targets != "[]" && -n $targets ]] || { fail "capstone.json: \"metrics\" lists no Prometheus endpoints"; return 1; }
  mkdir -p "$OPS_TMP"
  { echo "global: {scrape_interval: 1s, evaluation_interval: 1s}"; echo "scrape_configs:"
    jq -r 'to_entries[] | "  - job_name: capstone\(.key)\n    metrics_path: \(.value.path)\n    static_configs:\n      - targets: \(.value.hosts | tojson)"' <<<"$targets"
  } > "$CAP_TMP/prometheus.yaml"
  rm -rf "$OPS_TMP/prometheus"
  "$OPS_BIN/prometheus" --config.file="$CAP_TMP/prometheus.yaml" --storage.tsdb.path="$OPS_TMP/prometheus" \
    --web.listen-address=0.0.0.0:9090 > "$OPS_TMP/prometheus.log" 2>&1 & OPS_PIDS+=($!)
  OPS_DASHBOARDS="$CAP_TMP/no-dashboards" ops_start tempo prometheus grafana
}

probe_trace() {
  local p; p=$(mj .probes.crossSubgraph); [[ $p != null ]] || { fail "probes.crossSubgraph is missing"; return; }
  local tid sid svc
  tid=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n'); sid=$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')
  cap_send "$(jq -r .operation <<<"$p")" "$(jq -c '.variables // {}' <<<"$p")" "$(jq -r '.as // empty' <<<"$p")" "" -H "traceparent: 00-$tid-$sid-01" > "$CAP_TMP/trace-req.json"
  for _ in $(seq 1 30); do
    sleep 1
    svc=$(tempo_trace "$tid" 2>/dev/null | jq -c '[.trace.resourceSpans[]?.resource.attributes[]? | select(.key == "service.name") | .value.stringValue] | unique' 2>/dev/null)
    [[ $(jq 'length' <<<"${svc:-[]}") -ge 3 ]] && break
  done
  if [[ $(jq 'length' <<<"${svc:-[]}") -ge 2 ]]; then pass "tracing: one trace for $(jq -r .operation <<<"$p") holds spans from $svc"
  else fail "tracing: the trace $tid should hold spans from at least two services" "found ${svc:-none} (OTLP to http://localhost:4318, W3C traceparent propagated)"; fi
}

probe_dashboard() {
  local file; file=$(m .dashboard)
  [[ -s $LAB_DIR/$file ]] && jq -e '.panels | type == "array"' "$LAB_DIR/$file" >/dev/null 2>&1 || { fail "the dashboard $file is missing or has no panels"; return; }
  local imp; imp=$(jq '{dashboard: (. + {id: null}), overwrite: true}' "$LAB_DIR/$file" | curl -sS -X POST localhost:3000/api/dashboards/db -H 'Content-Type: application/json' -d @-)
  [[ $(jq -r .status <<<"$imp" 2>/dev/null) == success ]] && pass "Grafana imports $file" || fail "Grafana refused $file" "$(cut -c1-200 <<<"$imp")"
  local total=0 live=0 e x empty=()
  while IFS= read -r e; do
    [[ -n $e ]] || continue
    total=$((total + 1))
    x=$(sed 's/\${\{0,1\}__rate_interval}\{0,1\}/1m/g; s/\${\{0,1\}__interval}\{0,1\}/1m/g; s/\${\{0,1\}__range}\{0,1\}/5m/g' <<<"$e")
    if [[ $(promq "$x" 2>/dev/null | jq 'length' 2>/dev/null) -ge 1 ]]; then live=$((live + 1)); else empty+=("$(cut -c1-60 <<<"$e")"); fi
  done < <(jq -r '[.panels[], (.panels[] | .panels[]?)] | .[] | .targets[0].expr? // empty' "$LAB_DIR/$file")
  if (( total >= 4 && live * 4 >= total * 3 )); then pass "dashboard: $live of $total panels show data after the suite ran"
  else fail "dashboard: at least 4 panels, three quarters with data ($live of $total)" "empty: ${empty[*]}"; fi
}
