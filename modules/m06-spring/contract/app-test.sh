# Shared by Module 6 tests: source it after testlib.sh, from the directory under test.
#   app_build <jar>            mvn -q package (tests skipped), passes when <jar> exists
#   app_start <cmd...>         start the app in the background on $PORT (4000), wait until it listens
#   app_stop                   stop it and wait (also runs on exit)
#   app_hurl <file> [label]    run a Hurl file against $URL; on failure list the failing entries by their "# Xnn" titles
PORT=${PORT:-4000}; URL="http://localhost:$PORT/graphql"; APP_PID=
listening() { (exec 3<>/dev/tcp/localhost/$PORT) 2>/dev/null; }
app_build() {
  if mvn -q package -DskipTests >/tmp/app-build.log 2>&1 && [[ -f "$1" ]]; then pass "mvn package builds $1"; return 0; fi
  fail "mvn package failed"; grep -E "ERROR|error:" /tmp/app-build.log | head -12 | sed 's/^/        /'; return 1
}
app_start() {
  listening && { fail "port $PORT is already in use: stop whatever runs there first"; return 1; }
  "$@" >/tmp/app.log 2>&1 & APP_PID=$!
  trap app_stop EXIT
  for _ in $(seq 1 120); do listening && return 0; kill -0 $APP_PID 2>/dev/null || break; sleep 0.5; done
  fail "the app did not start on port $PORT" "$(grep -E 'ERROR|Exception|Caused by' /tmp/app.log | head -4)"; return 1
}
app_stop() { [[ -n "$APP_PID" ]] && { kill $APP_PID 2>/dev/null; wait $APP_PID 2>/dev/null; APP_PID=; }; return 0; }
app_hurl() {
  local file="$1" label="${2:-$(basename "$1")}" t failed
  t=$(grep -cE '^# [A-Z][0-9]+' "$file")
  if hurl --test --continue-on-error --variable "url=$URL" --variable "base=http://localhost:$PORT" "$file" >/tmp/hurl.log 2>&1; then pass "$label: all $t checks"; return 0; fi
  failed=$(grep -oE "$(basename "$file"):[0-9]+" /tmp/hurl.log | cut -d: -f2 | sort -un | while read -r n; do
    awk -v n="$n" 'NR<=n && /^# [A-Z][0-9]/ {l=$0} END {print l}' "$file"; done | sort -u)
  fail "$label: $(echo "$failed" | grep -c .) of $t fail"; echo "$failed" | sed 's/^/        /'; return 1
}
