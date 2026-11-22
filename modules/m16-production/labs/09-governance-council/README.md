# Game 16.9: Governance council

Lecture: [16.3 Governance](../../lectures/16.3-governance.md). Grading: rubric.

Four teams bring four schema changes to the review board. Each proposal in `proposals/` comes with:

- the change;
- the reason;
- the usage data the registry has, as in lab 16.2;
- the open questions.

The board decides each one:

- accept;
- accept with changes (say which);
- reject (with an alternative where there is one);
- defer (until what).

It records each decision in `DECISIONS.md`, with the reasons, the evidence they rest on, conditions, owners,
dates and any dissent.

| # | Proposal | From |
| --- | --- | --- |
| 1 | `Book.price: Float` | catalog |
| 2 | `Review.rating` becomes `Float` | reviews |
| 3 | replace `Query.search` with `searchBooks(input:)` | platform |
| 4 | paginate `Club.members` | community |

Use what the earlier labs built:

- the usage data: which clients, how often, which versions;
- the schema diff: is it breaking?
- the cost rule: is it bounded?
- the versioning policy from 16.7: how long a breaking change takes.

```bash
bin/course start m16/09
cd workspace/m16-production/09-governance-council
# write DECISIONS.md, then: bin/course grade m16/09
```

## Group variant (4 to 7 people, about 60 minutes)

Roles:

- **chair**: keeps time, 12 minutes per proposal;
- **proposer**: the owning team, a different person for each proposal;
- **client representatives**: web, iOS;
- **API product**: partners and the public changelog;
- **SRE**: cost and load.

For each proposal:

1. The proposer presents (2 min).
2. Each role asks one question (5 min).
3. Debate (3 min).
4. The chair calls the decision (2 min). Dissent is recorded, not argued further.

## Solo variant

Play every seat. For each proposal, first write the strongest case for it and the strongest case against it,
then decide. Leave a day between writing and deciding if you can.

## Done when

`DECISIONS.md` passes the rubric (`bin/course grade m16/09`).
