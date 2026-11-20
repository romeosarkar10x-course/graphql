#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Feed.java || finish
cd "$LAB_DIR"
for c in c1 c2 c3; do
  timeout 60 java -cp "$(lib_cp)" Feed.java "$LAB_HOME/tests/feed.graphql" "{\"club\":\"$c\"}" >/tmp/out.json 2>/tmp/err.txt
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/feed-$c.json"; then pass "feed for $c"
  else fail "feed for $c differs (< yours, > expected)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/feed-$c.json" | head -8
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
done
# The compiler as a test: add a record, the switch must stop compiling.
grep -q -- '-- new feed items go here --' Feed.java || { fail "keep the '// -- new feed items go here --' line in Feed.java"; finish; }
grep -qE 'sealed\s+interface\s+FeedItem' Feed.java && pass "FeedItem is a sealed interface" || fail "FeedItem should be a sealed interface"
rm -rf /tmp/feedc && mkdir -p /tmp/feedc/src /tmp/feedc/out && cp Feed.java Data.java Json.java /tmp/feedc/src/
sed -i 's|// -- new feed items go here --|record ClubRenamed(String clubId, String at, String newName) implements FeedItem {}|' /tmp/feedc/src/Feed.java
if javac -cp "$(lib_cp)" -d /tmp/feedc/out /tmp/feedc/src/*.java >/tmp/feedc/log 2>&1; then
  fail "adding a ClubRenamed record still compiles: the type resolver would fail at runtime instead" "use a switch over the sealed type with no default branch"
elif grep -q "does not cover all possible input values" /tmp/feedc/log; then
  pass "adding a FeedItem record breaks the build at the type resolver's switch"
else fail "adding a record failed to compile, but not because of the switch" "$(grep error /tmp/feedc/log | head -2)"; fi
notes_check notes.md 60 m03/07
finish
