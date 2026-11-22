source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
compose $FED/compose/supergraph.yaml /tmp/sg.graphql; subs_start; router_start apollo /tmp/sg.graphql /course/modules/m15-federation/labs/10-polyglot-kotlin/tests/router.yaml
gql http://127.0.0.1:4000/ '{ users { id } __type(name: "User") { fields { name } } }' | cut -c1-400; echo
gql http://127.0.0.1:4000/ '{ __type(name: "User") { fields { name } } }' | cut -c1-400
