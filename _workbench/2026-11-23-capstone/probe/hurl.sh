source /course/_workbench/2026-11-23-capstone/probe/lib.sh
cd $R
start_jar inventory 4001 || exit 1
start_jar lending 4002 || exit 1
scripts/router.sh > /tmp/router.log 2>&1 & PIDS+=($!)
for i in $(seq 30); do curl -fs -o /dev/null localhost:4000/health && break; sleep 0.5; done
tok() { java /course/modules/m10-security/contract/Token.java "$@" --aud toolshed-api; }
hurl --test --variables-file tests/ids.env --variable api=http://localhost:4000/graphql \
  --variable token_member=$(tok ada) --variable token_other=$(tok olu) --variable token_admin=$(tok root) --variable token_expired=$(tok ada --expired) \
  tests/api.hurl 2>&1 | tail -30
