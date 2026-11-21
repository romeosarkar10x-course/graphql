# Security testing toolkit: notes (reference)

## graphql-cop before and after

graphql-cop 1.15, 13 checks, through `bin/course seckit` (`scans/before.json`, `scans/after.json`, copies in
`_workbench/2026-11-21-m10-probe/`). Before (lab 10.7's server plus lab 10.4's limits): 4 HIGH findings.

| Finding | Why it fired | Fix |
|---|---|---|
| Alias Overloading | 101 aliases of `__typename` were answered: 101 fields is far under 500 | at most 30 aliases (`ShapeLimits`) |
| Field Duplication | 500 copies of `__typename`: exactly 500 fields, and the limit refused only *more* than 500 | field limit 200 |
| Directive Overloading | 10 unknown directives produced 10 validation errors: graphql-java validated each | at most 10 directives, none repeated on one element, one error |
| Introspection | `__schema` answered | `spring.graphql.schema.introspection.enabled: false` |

After: no finding. Lab 10.4's real screens stay far under every new limit (12 fields, no aliases). The
scan also found a bug the course had shipped: graphql-cop's first request is `{ __typename }`, which costs 0,
and Bucket4j refuses to consume 0 tokens, so lab 10.4's server answered 500 to the cheapest query there is.
Fixed in every lab (`Math.max(cost, 1)`), with a check (R08) in lab 10.4.

## Clairvoyance against your server with introspection off

Clairvoyance 2.5.5 recovered nothing (`_workbench/2026-11-21-m10-probe/clairvoyance.txt`): it reads
graphql-js's "Did you mean" suggestions, and graphql-java 26.1 neither suggests nor phrases errors the way
it expects ("Unknown error message for `valid_field`"). But graphql-java's errors still sort a wordlist for
anyone patient: an unknown name says `FieldUndefined`, a real one says `SubselectionRequired for type
'[Book!]!' of field 'books'` or `Missing field argument 'id'`, which names the type and the argument too.
Introspection off hides the schema from tools, not from a script. Trusted documents (lab 10.5) close it:
no ad-hoc document reaches validation at all.

## What the scanner missed that lab 10.3 found

Against the vulnerable fork graphql-cop reported alias overloading, directive overloading, field
duplication, introspection and GET queries, and missed: the `text/plain` CSRF (it only tries
url-encoded POSTs, which the fork refuses), the cookie token, `alg: none`, the SQL injection, the
`node(id:)` leak, the brute force, mass assignment, the error leak and the WebSocket origin. A scanner
checks configuration patterns; authorization and business logic need tests that know the app, which is
what the 13 regression tests are: 11 of them fail on the fork.

## The tools you did not run (InQL, DVGA, ZAP): what each is for

- **InQL** (Burp Suite extension): turns a schema (or introspection) into ready-made requests for every
  query and mutation, and finds cycles for DoS testing; for manual testing inside Burp.
- **DVGA** (Damn Vulnerable GraphQL Application): a deliberately vulnerable Python server to practise on,
  like this module's fork; useful because its bugs are documented.
- **OWASP ZAP**: a general web scanner with a GraphQL add-on that imports a schema and fuzzes arguments;
  it adds what generic scanners know (headers, injection payloads) to GraphQL endpoints.
None runs here: InQL needs Burp's GUI, ZAP and DVGA are large images. Their roles are what matters.
