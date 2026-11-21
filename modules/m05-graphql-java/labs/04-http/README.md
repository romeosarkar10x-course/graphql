# Lab 5.4 — GraphQL over HTTP, by the book

**Lecture:** [5.4 Serving over HTTP yourself](../../lectures/5.4-http.md) · **Grading:** auto + rubric · **Needs:** nothing

`Server.java` puts Shelfie on the JDK's own HTTP server (`com.sun.net.httpserver`) and hands every
request to `GraphQLHttp.handle(method, rawQuery, headers, body)`. The starter's handler is the version
most tutorials stop at: POST only, always 200, always `application/json`.

Make it pass the audit: 25 checks from the
[GraphQL-over-HTTP September 2026 edition](https://http-spec.graphql.org/September2026/),
the first published edition (2026-11-12). Each check in `tests/audit.hurl` quotes the rule it tests.

`bin/course test m05/04` starts your server, runs the audit and lists the checks that fail by name. To
poke at it by hand, server and client must share one container:

```
bin/course shell
source tools/testlib.sh; cd workspace/m05-graphql-java/04-http
java -cp "$(lib_cp)" Server.java 4000 &
curl -si localhost:4000/graphql -H 'Content-Type: application/json' -d '{"query":"{ hello boom }"}'
hurl --test --continue-on-error --variable url=http://localhost:4000/graphql /course/modules/m05-graphql-java/labs/04-http/tests/audit.hurl
```

What the audit expects, in short: GET and POST; content negotiation (`application/graphql-response+json`
first, `application/json` for legacy clients, 406 otherwise); 415, 400 and 422 for bad requests; 422 for
validation and variable errors; **294** for data plus errors; 405 with `Allow` for mutations over GET and
for other methods. The X-Shelfie-User header still becomes the `viewer` in GraphQLContext.

## Bonus: the same handler behind Javalin

`javalin/` builds `JavalinServer` with Javalin 7.2.4 and the same `GraphQLHttp`. When you remove its
TODO, the test runs the audit against it too and reports the result (not graded).

## notes.md

Audit score before and after, and four questions.

## Done when

`bin/course test m05/04` is green and `bin/course grade m05/04` gives 7/10 or more.
