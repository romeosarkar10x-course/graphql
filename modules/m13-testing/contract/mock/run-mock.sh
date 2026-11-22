# Shared by lab 13.4 and mini-project 13.10 tests: source after testlib.sh.
#   mock_start <port> <cmd...>   start a mock server in the background, wait until <port> listens
#   mock_stop                    stop every mock server started (also runs on exit)
#   mock_check <port> <schema> <queries> [--realistic]   run MockCheck; pass/fail with its problem lines
MOCK_PIDS=()
mock_start() {
  local port="$1"; shift
  "$@" >/tmp/mock-$port.log 2>&1 & MOCK_PIDS+=($!)
  trap mock_stop EXIT
  for _ in $(seq 1 120); do (exec 3<>/dev/tcp/localhost/$port) 2>/dev/null && return 0; kill -0 ${MOCK_PIDS[-1]} 2>/dev/null || break; sleep 0.5; done
  fail "the mock server did not listen on :$port" "$(grep -vE '^\s*\^|warning' /tmp/mock-$port.log | head -4)"; return 1
}
mock_stop() { local p; for p in "${MOCK_PIDS[@]}"; do kill "$p" 2>/dev/null; wait "$p" 2>/dev/null; done; MOCK_PIDS=(); return 0; }
mock_check() { # $1 port, $2 schema, $3 queries, $4 label, rest MockCheck flags
  local port="$1" schema="$2" queries="$3" label="$4" out; shift 4
  out=$(java -cp "$(lib_cp)" /course/modules/m13-testing/contract/mock/MockCheck.java "http://localhost:$port/graphql" "$schema" "$queries" --quiet "$@" 2>/dev/null)
  if [[ $? -eq 0 ]]; then pass "$label: $(tail -1 <<<"$out")"; return 0; fi
  fail "$label: $(tail -1 <<<"$out")"; grep '^FAIL' <<<"$out" | head -8 | sed 's/^FAIL /        /'; return 1
}
