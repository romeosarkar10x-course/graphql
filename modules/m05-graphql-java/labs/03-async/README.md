# Lab 5.3 — Ten slow siblings

**Lecture:** [5.3 Async execution](../../lectures/5.3-async-execution.md) · **Grading:** auto + rubric · **Needs:** nothing

`Siblings.java` builds a schema with `n` sibling fields (default 10). Each fetcher makes a 200 ms
blocking call (`slowCall`, a stand-in for JDBC or HTTP).

```
bin/course java Siblings.java <sync|platform|virtual|mutation> [n]
sync n=10 2003 ms
```

| Mode | Fetcher | Executor | Expected |
|---|---|---|---|
| `sync` | blocks the calling thread (given) | none | ~2,000 ms |
| `platform` | returns a `CompletableFuture` at once | fixed pool of 4 OS threads | ~600 ms (3 waves) |
| `virtual` | the same | one virtual thread per task | ~200 ms, also for n = 1000 |
| `mutation` | the same, as a `mutation` | virtual | n x 200 ms, fields in order |

The starter's async modes block like `sync`, and its `virtual` mode uses the fixed pool. Fix both TODOs.
Then fill the table in `notes.md` (estimate `platform 1000` before you run it: it takes a while).

## Done when

`bin/course test m05/03` is green and `bin/course grade m05/03` gives 7/10 or more.
