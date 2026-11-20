# Lab 4.1 — First mutations

**Lecture:** [4.1 Mutation basics](../../lectures/4.1-mutation-basics.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up --rebuild` (the reference server gained mutations)

The reference server now writes. Mutations act as the user named in the `X-Shelfie-User` header; the
course runner puts the data back to the seed before every test (`bin/course ref reset` does it by hand).

## Part 1: four mutations, four requests

`FirstMutations.java`, as Dev Patel (`u4`), in this order: join club `c2`, review book `b2` (rating 5,
any body of 10+ characters), delete that review, leave `c2`. Print exactly:

```
joined Slow Burn Mysteries: 5 members
review r16 on Piranesi: average now 4.67
deleted r16: average back to 4.5
left Slow Burn Mysteries: 4 members
```

Every number comes from the **mutation's own payload**. The test counts requests on the server: four.

## Part 2: serial vs parallel, measured

`SerialVsParallel.java` runs `{ a: slow b: slow c: slow }` as a query and as a mutation, each field
sleeping 200 ms, and logs start and end times. The starter's query takes 600 ms like the mutation.
Fix the fetcher (TODO) so the query's fields overlap, and see that the mutation's still do not.

## Done when

`bin/course test m04/01` is green and `bin/course grade m04/01` gives 7/10 or more.
