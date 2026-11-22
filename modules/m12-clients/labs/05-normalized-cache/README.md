# Lab 12.5: A normalised cache

Lecture: [12.5 Normalised caching](../../lectures/12.5-normalised-caching.md). Grading: auto + rubric.
Needs the reference server for the live experiment.

Write `NormalizedCache.java` (about 200 lines, graphql-java's parser and Jackson on the class path): store
GraphQL responses as flat records, one per entity, linked by references, and answer later documents from them.
The contract is [contract/cache/SPEC.md](../../contract/cache/SPEC.md); scenarios 01 to 10 in
[contract/cache/scenarios/](../../contract/cache/scenarios/) are this lab's tests:

| | Scenario |
|---|---|
| 01–03 | a hit, a miss, a miss because one field is missing |
| 04 | **the experiment:** two queries show book b1, one mutation renames it, both see the new title |
| 05–06 | arguments and variables in the storage key; aliases are not |
| 07–08 | lists of entities become lists of references; objects without an id stay embedded |
| 09–10 | stored nulls are hits; writes to one entity merge |

```bash
bin/course start m12/05
cd workspace/m12-clients/05-normalized-cache
bin/course test m12/05
# one scenario by hand:
bin/course shell -c "mkdir -p /tmp/c && cp NormalizedCache.java /course/modules/m12-clients/contract/cache/Scenarios.java /tmp/c \
  && java -cp \"\$(ls /course/.cache/course-lib/*.jar | paste -sd:)\" /tmp/c/Scenarios.java /course/modules/m12-clients/contract/cache/scenarios 04-04"
```

The live experiment (`contract/cache/LiveDemo.java`, run by the test) does scenario 04 against Shelfie:
it caches a book page and a club page that both show b1's average rating, posts one review, writes the
mutation's result into your cache, and checks that both pages now show Shelfie's new average, with three
requests in all.

In `notes.md`: the store from the live run, what your cache cannot know without the schema, and what one
mutation result does not update.

## Done when

`bin/course test m12/05` passes (10 scenarios, 4 live checks) and `notes.md` passes the rubric
(`bin/course grade m12/05`). Mini-project [m12/09](../09-project-cache/README.md) takes this cache to 25 scenarios.
