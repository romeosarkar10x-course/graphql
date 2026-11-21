#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
require_file Scroller.java || finish
rm -rf /tmp/scroll && mkdir -p /tmp/scroll
javac -cp "$(lib_cp)" -d /tmp/scroll Scroller.java "$LAB_HOME/tests/ScrollJudge.java" 2>/tmp/scroll.log \
  || { fail "Scroller.java does not compile with the judge" "$(grep error /tmp/scroll.log | head -3)"; finish; }
listening() { (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null; }
round() { # $1 label, rest: FeedServer options
  local label="$1"; shift
  java -cp "$(lib_cp)" /course/modules/m08-pagination-errors/contract/FeedServer.java "$@" >/tmp/feed.log 2>&1 & local pid=$!
  for _ in $(seq 60); do listening && break; sleep 0.5; done
  out=$(java -cp "/tmp/scroll:$(lib_cp)" ScrollJudge http://localhost:4000/graphql "$label" 2>&1 | tail -1)
  kill $pid; wait $pid 2>/dev/null
  if [[ "$out" == ok* ]]; then pass "${out#ok    }"; else fail "${out#FAIL  }"; fi
}
round "level 1, a new book every 20 ms" --every 20
round "level 2, new books and deletes every 20 ms" --every 20 --deletes
finish
