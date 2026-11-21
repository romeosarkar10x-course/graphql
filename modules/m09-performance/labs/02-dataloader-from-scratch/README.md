# Lab 9.2 — A DataLoader from scratch

**Lecture:** [9.2 DataLoader from scratch](../../lectures/9.2-dataloader-from-scratch.md) · **Grading:** auto · **Needs:** nothing

`MiniLoader<K, V>` is a DataLoader in one file, about 60 lines, on `CompletableFuture` with manual
dispatch. The starter is the loader that causes N+1: every `load` is its own batch, sent at once.

## The contract

- `load(key)` returns a future and sends nothing. `dispatch()` sends every queued key in **one** call to
  the batch function, in the order the keys were first asked for, and returns how many keys it sent.
  Nothing queued: no call, returns 0.
- A key asked for twice is sent once, and both callers get the same value. A key that has been loaded
  is remembered: loading it again sends nothing.
- The batch function answers with one `Result` per key, in the same order. A `Result.fail` fails that
  key only. A failed future, an exception thrown by the batch function itself, or the wrong number of
  results fails every key in that batch. `dispatch()` itself never throws.
- A failure is **not** remembered: the next `load` of that key tries again (java-dataloader 6.0.0
  behaves the same way, lecture 9.2).
- `new MiniLoader<>(batch, maxBatchSize)` splits a dispatch into batches of at most that many keys.
- `prime(key, value)` seeds a value without a batch; an existing entry wins. `clear(key)` forgets one key,
  `clearAll()` all of them.
- A `load` made while a batch is completing (in a `thenAccept`, say) waits for the next `dispatch()`.
- `loadMany(keys)` answers in the order asked, duplicates included.

## Do

Make `MiniLoader.java` meet the contract. Try it from a `main` or jshell as you go:
`bin/course java MiniLoader.java` compiles it.

The suite (`bin/course test m09/02`) has 16 cases and names each one. Write your own small test for
each rule above before you run it: the suite is the hidden test, and reading it first takes the
exercise away.

## Done when

`bin/course test m09/02` passes all 16 cases.
