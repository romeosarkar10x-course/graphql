q() { # label, query, [variables], [operationName]
  echo "### $1"; echo "$2"
  jq -n --arg q "$2" --argjson v "${3:-null}" --arg o "${4:-}" '{query:$q, variables:$v} + (if $o=="" then {} else {operationName:$o} end)' \
   | curl -sS -H 'Content-Type: application/json' -H 'Accept: application/graphql-response+json' -w '\nHTTP %{http_code}\n' --data-binary @- http://shelfie-ref:8080/graphql | jq -c . 2>/dev/null || true
  echo
}
q search '{ search(text: "the") { __typename ... on Book { title } ... on Club { name } ... on User { handle } } }'
q alias-ok '{ a: club(id: "c1") { name } b: club(id: "c2") { name } }'
q alias-collision '{ x: club(id: "c1") { name } x: club(id: "c2") { name } }'
q alias-collision-field '{ club(id: "c1") { name: description name } }'
q fragment-cycle '{ club(id: "c1") { ...A } } fragment A on Club { owner { clubs { ...A } } }'
q fragment-unused '{ hello } fragment A on Club { name }'
q fragment-wrong-type '{ club(id: "c1") { ...B } } fragment B on Book { title }'
q two-ops-no-name 'query A { hello } query B { clubs { name } }'
q two-ops-named 'query A { hello } query B { clubs { name } }' null B
q skip-subscription-root 'subscription S($off: Boolean = false) { meetupScheduled(clubId: "c1") @skip(if: $off) { id } }'
q include-subscription-nested 'subscription S($on: Boolean = true) { meetupScheduled(clubId: "c1") { id title @include(if: $on) } }'
q missing-required-var 'query C($id: ID!) { club(id: $id) { name } }' '{}'
q var-default 'query C($id: ID! = "c2") { club(id: $id) { name } }' '{}'
q var-unused 'query C($id: ID!) { hello }' '{"id":"c1"}'
q var-wrong-type 'query C($id: ID!) { club(id: $id) { members(first: $id) { name } } }' '{"id":"c1"}'
q injected-format '{ club(id: "") { __schema { types { name } } } #") { name } }'
q injected-format2 '{ club(id: "") { name } __schema { types { name } } #") { name } }'
q unknown-field '{ club(id: "c1") { title } }'
q missing-subselection '{ club(id: "c1") { owner } }'
q scalar-subselection '{ club(id: "c1") { name { x } } }'
q typename-root '{ __typename }'
q type-count '{ __schema { types { name } } }'
q nonexistent-club '{ club(id: "zz") { name } }'
q directive-unknown '{ hello @uppercase }'
q directive-wrong-location 'query Q @include(if: true) { hello }'
q skip-and-include '{ hello @skip(if: false) @include(if: false) }'
q op-description '"Card" query Card { hello }'
q fragment-args '{ ...F(x: 1) } fragment F($x: Int = 0) on Query { hello }'
