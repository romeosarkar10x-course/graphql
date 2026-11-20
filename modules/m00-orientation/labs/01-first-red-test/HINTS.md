# Hints, lab 0.1

1. Run `bin/course hurl hello.hurl --test` from your lab folder and read the whole failure block, not just the red word. Hurl shows both the value it expected and the value it got.
2. The server is not wrong; the test's promise is. Change the expected value to what the server really says. Then think: what would a GraphQL server return if `{ hello }` were *invalid*? Would the status code catch it?
3. A GraphQL validation error still comes back as HTTP 200 with `application/json`. The only thing that tells you it failed is the presence of a top-level `errors` key. Hurl has a predicate for "this path is absent".
