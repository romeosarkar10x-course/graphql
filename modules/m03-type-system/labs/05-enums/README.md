# Lab 3.5 — Enums inside and out

**Lecture:** [3.5 Enums](../../lectures/3.5-enums.md) · **Grading:** auto + rubric · **Needs:** nothing

An enum has a name clients see (`PRIVATE`) and a value your Java code sees. They do not have to
match: the legacy clubs table stores `P` and `X`. And adding a value is a breaking change for every
client that decodes strictly.

## Do

1. **TODO 1, `Visibility`:** wire an `EnumValuesProvider` so `PUBLIC` ↔ `"P"` and `PRIVATE` ↔ `"X"`.
   The rows keep their codes; `clubs(visibility: PRIVATE)` must filter correctly.
2. **TODO 2, `ReadingStatus`:** bind it to the Java enum with `NaturalEnumValuesProvider`. Run the
   shelf query for `u3` against `schema.graphqls`: the database already contains `ABANDONED`. Read the
   error, and see what `data` is (the test expects exactly that response: the v1 server fails loudly).
3. Write `schema-v2.graphqls`: `schema.graphqls` plus `ABANDONED` with a description. Nothing else.
   `Enums.java --v2` serves it.
4. Save the v2 response for u3 and run `OldClient.java` on it (given; a client shipped against v1 with
   Jackson's defaults). Then write `TolerantClient.java`, the same client mapping unknown values to an
   `UNKNOWN` constant: the test runs it on v1 and v2 responses.
5. `notes.md`.

## Done when

`bin/course test m03/05` is green and `bin/course grade m03/05` gives 7/10 or more.
