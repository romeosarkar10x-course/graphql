# Mini-project 1.6 — Latency lab

**Module 1 mini-project** · **Grading:** auto (model) + rubric (page) · About 2 hours

Turn what you measured in labs 1.2 and 1.3 into a model, then into a page that lets anyone feel it.

## Part 1: the model (auto-graded)

Implement `LatencyModel.java` (signatures and the formula are in the file). A page is a map from call
name to the calls it depends on. Calls go out in **waves**: a call's wave is 1 + the deepest wave of its
dependencies. Each wave costs `ceil(callsInWave / maxParallel)` round trips of `rttMs + serverMs`, and the
bytes cost `totalBytes × 8 / kbps` milliseconds. Reject cycles, unknown dependencies and silly networks
with `IllegalArgumentException`.

`bin/course test m01/06` compiles it with 11 JUnit tests (in `tests/`, readable: they are the spec).

## Part 2: the slider page (rubric)

A single `index.html`, no build step, that shows the club page's REST time and GraphQL time as two bars,
updating live as four sliders move: round trip, server time per call, bandwidth, parallel requests.
Use: REST = waves of 1, 9 and 3 calls, about 450 bytes each; GraphQL = 1 call, 824 bytes. Open it
straight from disk in a browser.

Then find, and write at the bottom of the page or in a `notes.md`:
1. the round-trip time at which GraphQL first wins with the other sliders at default, and
2. a setting where REST wins, and why.

## Group

Everyone builds the page alone in 45 minutes, then the group votes on the clearest one. The winner
presents the answer to question 2.
