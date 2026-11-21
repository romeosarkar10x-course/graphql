# Hints, lab 10.8

1. Read how graphql-cop decides (`/opt/graphql-cop/lib/tests/` in the seckit image:
   `bin/course seckit sh -c 'cat /opt/graphql-cop/lib/tests/dos_alias_overloading.py'`). Each check is one
   request and one condition on the answer; fix the cause, not the condition.
2. `QueryComplexityLimits` counts fields but not aliases or directives. A `PreparsedDocumentProvider` sees
   the parsed `Document` after validation (`parseAndValidate.apply(input)`), and `NodeTraverser` with a
   `NodeVisitorStub` walks it.
3. For tests that must fail on the fork, pick behaviour the fork gets wrong for anonymous callers:
   `text/plain` bodies, unsigned tokens, `node(id:)` on a private review, socket origins, masked errors,
   and every demand limit.
