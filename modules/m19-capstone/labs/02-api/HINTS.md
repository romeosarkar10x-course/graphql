# Hints, stage 2

1. Get one subgraph serving one query from the contract file before anything else: Maven's `<resources>` can
   copy `../contract/<name>.graphqls` into `graphql/`, so the service cannot drift from the contract. Then
   `curl` it with `{ _service { sdl } }`.
2. If "the running schema differs from the contract", the checker prints the coordinates that differ: a
   field added in code but not in the contract, an argument with another default, a nullability change.
   Fix the contract or the code; never both at once.
3. A probe that fails on the second run but not the first is state: your service kept data from the last run,
   or two runs used the same idempotency key. Load the seed at start, and put `{{run}}` in the probe's keys and
   names.
