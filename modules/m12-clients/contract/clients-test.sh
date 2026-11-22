# Shared by Module 12 tests: source after testlib.sh and m06's app-test.sh.
#   chat_start [port]      build (once) and start contract/chat-server (Module 11's chat) on port (4100)
#   chat_stop              stop it
#   chat_send <room> <author> <body>   sendMessage; prints the new message id
#   toxi_proxy <name> <port>           (re)create a Toxiproxy proxy toxiproxy:<port> -> shelfie-ref:8080, no toxics
#   toxi_toxic <name> <json>           add a toxic to that proxy
#   toxi_clear <name>                  remove the proxy
M12=/course/modules/m12-clients/contract
CHAT_PORT=4100; CHAT_PID=
chat_jar() {
  local jar=$M12/chat-server/target/shelfie-live.jar
  if [[ ! -f $jar || -n $(find $M12/chat-server/src $M12/chat-server/pom.xml -newer $jar -print -quit) ]]; then
    (cd $M12/chat-server && mvn -q package -DskipTests >/tmp/chat-build.log 2>&1) || { echo "chat server build failed: $(grep ERROR /tmp/chat-build.log | head -3)" >&2; return 1; }
  fi
  echo $jar
}
chat_start() {
  CHAT_PORT=${1:-4100}; local jar
  jar=$(chat_jar) || return 1
  java -jar $jar --server.port=$CHAT_PORT >/tmp/chat.log 2>&1 & CHAT_PID=$!
  for _ in $(seq 1 120); do (exec 3<>/dev/tcp/localhost/$CHAT_PORT) 2>/dev/null && return 0; sleep 0.25; done
  echo "chat server did not start: $(tail -3 /tmp/chat.log)" >&2; return 1
}
chat_stop() { [[ -n "$CHAT_PID" ]] && { kill $CHAT_PID 2>/dev/null; wait $CHAT_PID 2>/dev/null; CHAT_PID=; }; return 0; }
chat_send() {
  curl -s -H 'Content-Type: application/json' localhost:$CHAT_PORT/graphql \
    -d "$(jq -nc --arg r "$1" --arg a "$2" --arg b "$3" '{query:"mutation($r: ID!, $a: String!, $b: String!) { sendMessage(roomId: $r, author: $a, body: $b) { id } }", variables:{r:$r, a:$a, b:$b}}')" \
    | jq -r .data.sendMessage.id
}
toxi_proxy() {
  curl -s -o /dev/null -X DELETE $TOXI_API/proxies/$1
  curl -fsS -o /dev/null -X POST $TOXI_API/proxies -d "{\"name\":\"$1\",\"listen\":\"0.0.0.0:$2\",\"upstream\":\"shelfie-ref:8080\"}"
}
toxi_toxic() { curl -fsS -o /dev/null -X POST $TOXI_API/proxies/$1/toxics -d "$2"; }
toxi_clear() { curl -s -o /dev/null -X DELETE $TOXI_API/proxies/$1; }
#   capture_start <port> <log>         start contract/Capture.java (logs each request body, forwards to Shelfie)
#   capture_stop
CAP_PID=
capture_start() {
  java $M12/Capture.java $1 $REF/graphql > $2 2>/dev/null & CAP_PID=$!
  for _ in $(seq 1 40); do (exec 3<>/dev/tcp/localhost/$1) 2>/dev/null && return 0; sleep 0.25; done; return 1
}
capture_stop() { [[ -n "$CAP_PID" ]] && { kill $CAP_PID 2>/dev/null; wait $CAP_PID 2>/dev/null; CAP_PID=; }; return 0; }
