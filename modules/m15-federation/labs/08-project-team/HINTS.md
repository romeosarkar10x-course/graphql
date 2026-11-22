# Hints: m15/08

1. Every new field lives in exactly one schema file, of the team in the table. Reviews can add `topReview` to
   `Book` because it already has a `Book @key(fields: "id")` stub; clubs can return a `Book` and a `User` by
   constructing key-only values and letting the router fetch the rest.
2. A mutation that works with the subgraph called directly and refuses through the router: the router forwards no
   header unless told to. For compose: `depends_on` with `condition: service_healthy` (subgraphs need a
   healthcheck) and `service_completed_successfully` (one-shot jobs: the build, the supergraph).
3. The top review compares two things in order: rating, then the review's number (`r12` is newer than `r3`, so
   compare numbers, not strings). If `docker compose up` cannot find the router or Node.js, they are in the
   repository's `.cache`, mounted from `COURSE_ROOT`.
