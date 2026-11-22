source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
L=/course/modules/m15-federation/labs/04-java-subgraphs/solution
SUB_JAR=$L/target/shelfie-java-subgraphs.jar sub_start catalog 4002; SUB_JAR=$L/target/shelfie-java-subgraphs.jar sub_start reviews 4003
gql http://localhost:4002/graphql '{ _service { sdl } }' | jq -r .data._service.sdl | grep -n "key" | head
gql http://localhost:4003/graphql 'query($r: [_Any!]!) { _entities(representations: $r) { ... on Review { id rating body } } }' '{"r":[{"__typename":"Review","id":"r3"},{"__typename":"Review","id":"r404"}]}' | head -c 600; echo
gql http://localhost:4003/graphql 'query($r: [_Any!]!) { _entities(representations: $r) { ... on Book { id reviewCount averageRating } } }' '{"r":[{"__typename":"Book","id":"b1"},{"__typename":"Book","id":"nope"}]}' | head -c 600; echo
grep -m5 -A3 "Exception" /tmp/sub-reviews.log | cut -c1-250
