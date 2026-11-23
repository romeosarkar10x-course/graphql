# Hints, stage 1

1. Start with the screens, not the types: draw (or list) four or five screens and write the operation each one
   needs, field by field. The types are what those operations select; anything no screen selects can wait.
2. The required shapes come from the product if you look for them: a list that grows is a connection (what do
   people filter it by, and in what order do they want it?); "this or that, with nothing in common" is a union;
   "these kinds share some fields" is an interface; "find it by one of several keys" is a `@oneOf` input; "this
   can fail in ways the user should see" is a result union. If one of them feels forced, the brief may be too
   small: add a screen rather than a type.
3. If `bin/course test m19/01` says the contract does not compose, run the composer yourself on a two-line
   `supergraph.yaml` (`source /course/modules/m15-federation/contract/fed.sh; compose supergraph.yaml /tmp/s.graphql`)
   and read the first error code. If operations do not validate, the message names the field: a fragment in
   another file needs its file in `operations/`, and every operation needs a name.
