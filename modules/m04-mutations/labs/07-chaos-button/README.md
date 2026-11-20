# Game 4a — The chaos button

**Lecture:** after [4.4 Idempotency, concurrency, bulk](../../lectures/4.4-idempotency.md) · **Grading:** auto + rubric · **Needs:** nothing · **Kind:** game

`Points.java` is a reading-challenge points service: award points, transfer them, redeem rewards with
limited stock. It passes every test you would write by hand. The chaos button sends it 2,000 mutations
from 16 concurrent clients through a bad network:

| Chaos | Rate | What the client does |
|---|---|---|
| lost response | 20% | The mutation ran, the answer vanished; the client retries with the **same `requestId`** |
| duplicated request | 10% | Two copies arrive at once |
| slow retry | 10% | A retry arrives after the answer did |

Then it checks what must always hold: every retry got the same answer as the first attempt; the
points in all balances equal 800 + awards − redemptions *that clients were told succeeded*; no balance
or stock below zero; stock taken equals redemptions reported.

## Run it

```bash
cd workspace/m04-mutations/07-chaos-button
../../../bin/course shell -c 'cd workspace/m04-mutations/07-chaos-button && mkdir -p /tmp/c &&
  javac -cp "$(source /course/tools/testlib.sh; lib_cp)" -d /tmp/c Points.java ../../../modules/m04-mutations/labs/07-chaos-button/tests/Chaos.java &&
  java -cp "/tmp/c:$(source /course/tools/testlib.sh; lib_cp)" Chaos 1'
```

`Chaos 0 --calm` is the hand-written test: one client, no chaos. The starter passes it.

## Win

Seeds 1 to 5 all pass (`bin/course test m04/07`). Then `notes.md`.

## Group variant

Everyone hardens their own `Points.java`. Then each player adds one new chaos mode to their copy of
`Chaos.java` (ideas: retries after a delay with a *new* `requestId`, a client that sends transfers
in both directions at once, a reward restock in the middle) and runs it against the others' services.
A point for each service your mode breaks; a point for surviving each mode.
