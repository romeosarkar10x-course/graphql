# Module 1 — Why GraphQL exists

About 3 hours, 5 lectures. By the end you can explain what GraphQL fixes, what it costs, and when REST,
gRPC, tRPC or OData is the better call, with numbers you measured yourself.

## Before you start

`bin/course ref up` (the reference server serves the same data as REST, GraphQL and gRPC, plus Toxiproxy).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 1.1 | [Where GraphQL came from](lectures/1.1-origins.md) | [m01/01 Card sort: 12 milestones](labs/01-timeline/README.md) | auto + rubric |
| 1.2 | [The pain it targets](lectures/1.2-the-pain.md) | [m01/02 The REST waterfall](labs/02-rest-waterfall/README.md) | auto + rubric |
| 1.3 | [The mental model](lectures/1.3-mental-model.md) | [m01/03 One GraphQL call](labs/03-one-graphql-call/README.md) | auto + rubric |
| 1.4 | [GraphQL vs REST vs gRPC vs tRPC vs OData](lectures/1.4-comparison.md) | [m01/04 Three styles, one domain](labs/04-three-styles/README.md) | auto + rubric |
| 1.5 | [When not to use it, and who does](lectures/1.5-when-not.md) | [m01/05 When not to](labs/05-when-not-to/README.md), [m01/09 Case-study teardown](labs/09-case-study-teardown/README.md) | rubric |

The shared contract for 1.2 and 1.3: [contract/club-page.md](contract/club-page.md).

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m01/06 Latency lab](labs/06-project-latency-lab/README.md) (mini-project) | Model (JUnit) + slider page | Build alone, vote on the clearest page |
| [m01/07 REST or GraphQL? cards](labs/07-game-rest-or-graphql/README.md) | 20 picks, AI-graded | Teams argue, challenge, first to 15 |
| [m01/08 Debate: bank goes GraphQL-first](labs/08-debate/README.md) | Both sides + verdict | Timed debate, then swap sides |

## Sources

[Facebook's 2015 announcement](https://engineering.fb.com/2015/09/14/web/graphql-a-data-query-language/),
[GitHub's 2016 launch post](https://github.blog/2016-09-14-the-github-graphql-api/),
[PayPal Checkout story](https://medium.com/paypal-engineering/3482f724fb53); companies in `plan/company-case-studies.md`.

## Checkpoint

[checkpoint.md](checkpoint.md), then `bin/course checkpoint 01`: the comparison numbers (labs 1.2 and 1.3
notes) and the latency-lab model must be in `workspace/`.
