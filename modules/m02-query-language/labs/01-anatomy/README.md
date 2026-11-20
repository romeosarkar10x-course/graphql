# Lab 2.1 — Anatomy of a query

**Lecture:** [2.1 Anatomy of a query](../../lectures/2.1-anatomy.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

Write one query and map its response onto Java records that mirror the selection set. Then break the
mirror on purpose and watch what Jackson does.

## Do

1. `bin/course start m02/01`, then work in `workspace/m02-query-language/01-anatomy/`.
2. In GraphiQL (http://localhost:8080/graphiql, variables `{"id": "c1"}`), extend `club-card.graphql`
   until it returns everything the card needs: the club's name, visibility and member count, the
   owner's name and handle, the current book's title and authors.
3. In `ClubCard.java`, add the record fields and the `Owner` and `Book` records, then finish `card()`.
   Run it: `../../../bin/course java ClubCard.java c1`. It must print exactly:

   ```
   The Midnight Readers [PUBLIC] · 8 members
   owner    Kemi Adeyemi (@kemia)
   reading  The Left Hand of Darkness, by Ursula K. Le Guin
   ```

   A club with no current book prints `reading  nothing yet`. An unknown id (`zz`) prints `no club zz`
   and exits with status 3; the HTTP code and that branch are already written.
4. Run the three experiments in `notes.md` (undo each one before the next).

## Done when

`bin/course test m02/01` is green and `bin/course grade m02/01` gives 7/10 or more.
