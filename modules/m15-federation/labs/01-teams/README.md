# Lab 15.1: One graph, many teams

Lecture: [15.1 One graph, many teams](../../lectures/15.1-teams.md). Grading: auto + rubric. Needs nothing.

Shelfie is one schema, one service and one team ([contract/monolith.graphqls](../../contract/monolith.graphqls)).
Four teams are about to own it: **accounts**, **catalog**, **reviews** and **clubs**. Write the map in
`ownership.json`:

```json
{
  "types":  { "User": "accounts", ... },              // every object type has an owning team
  "fields": { "Query.me": "accounts", "Book.reviews": "reviews", ... },
                                                       // every Query and Mutation field, and any field a team
                                                       // other than the type's owner provides
  "keys":   { "User": "id", ... }                      // how other teams point at a type
}
```

The grader checks the map's shape, not your taste: every owner is one of the four teams, every type and every
root field has an owner, each team owns at least one `Query` field, no team owns more than half of the fields,
and **every type that another team points at or adds fields to has a key** made of its own fields (that is the
rule federation will enforce in lab 15.3).

Then the experiment, in `notes.md`: the next feature is **club book of the month** (each club picks a book; the
club page shows its title, its top review and the reviewer's name). Estimate its coordination cost under three
models: the monolith as it is, a backend-for-frontend over four services, and a federated graph. Count PRs,
teams that must agree on a contract, meetings, deploys and their order, and who waits for whom.

```bash
bin/course start m15/01
cd workspace/m15-federation/01-teams
bin/course test m15/01
```

## Done when

`bin/course test m15/01` passes and `notes.md` passes the rubric (`bin/course grade m15/01`).
