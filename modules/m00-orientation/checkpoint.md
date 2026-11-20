# Module 0 checkpoint

Answer each out loud or in writing before opening the answer.

**1. Your Hurl test asserts `HTTP 200` and `jsonpath "$.data.hello" exists`. Is that enough to catch a validation error?**

<details><summary>Answer</summary>

It catches this one (an invalid query has no `data.hello`), but by accident. Assert `$.errors not exists`
explicitly: with `application/json`, a failed GraphQL request is still a 200.
</details>

**2. What is the difference between `bin/course test m00/02` and `bin/course test m00/02 --solution`?**

<details><summary>Answer</summary>

The first grades your copy in `workspace/`; the second grades the reference solution, to show the
tests themselves pass.
</details>

**3. Name the three body keys a GraphQL-over-HTTP POST can carry besides `extensions`, and which is required.**

<details><summary>Answer</summary>

`query` (required), `variables`, `operationName`.
</details>

**4. The reference server answers a GET with 405. Bug or allowed?**

<details><summary>Answer</summary>

Allowed. Servers MUST support POST and MAY support GET.
</details>

**5. Same invalid query, two `Accept` headers: `application/json` gives 200, `application/graphql-response+json` gives 400. Why keep the legacy behaviour at all?**

<details><summary>Answer</summary>

Old clients treat any non-2xx as a transport failure and never read the `errors` body. The new media
type is opt-in: clients that send it get meaningful status codes, old clients keep working.
</details>

**6. Where does Maven keep downloaded libraries in this course, and why there?**

<details><summary>Answer</summary>

In `.cache/m2/` inside the repo (set by `MAVEN_ARGS` in the image). Containers are thrown away after each
run (`--rm`); a cache inside the mounted repo survives, so Maven does not re-download everything.
</details>

**7. `film(id: ID!): Film` — which part is required, and what can be null?**

<details><summary>Answer</summary>

The `id` argument is required. The result `Film` may be null (for example when no film has that id).
</details>

**8. In jshell, why does a builder chain written with leading dots fail?**

<details><summary>Answer</summary>

jshell treats each complete line as a statement. `var r = HttpRequest.newBuilder(uri)` is complete, so
it runs, and the next line `.header(...)` is a syntax error. Put the dot at the end of the line.
</details>
