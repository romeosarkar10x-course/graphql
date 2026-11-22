# Report: an LLM against Shelfie

Reference notes. The sample run in `llm/` is not a fair measurement: the queries were written by Claude Opus 5.5
while it built this lab, with the schema and the ten described operations in context and after it had already
queried the reference server's data. Only one run was made, so there is no "schema only" row. Treat the table
as the shape of an answer, and measure your own model.

## The runs

| Run | Model | Valid | Answered | Total cost |
| --- | --- | --- | --- | --- |
| Schema and operations (authoring sample) | Claude Opus 5.5, one query per question, no execution feedback | 10 | 9 | 835 |
| The reference queries (`reference/`, hand-tuned) | — | 10 | 10 | 1,527 |

Scores from `agent score` on 2026-11-23 (`_workbench/2026-11-23-m18-probe/agent/llm-sample-score.md`).

## Failed and costly queries

- **q09 failed: a hidden default.** "When did The Midnight Readers hold their October wrap-up meetup?" The query
  asked for `meetups` without arguments. `meetups(upcomingOnly: Boolean = true)` drops past meetups, the wrap-up
  was on 2026-10-30, so the response was valid, cheap (cost 6) and silently empty of the answer. This is the
  failure that matters for agents: no error, a confident wrong answer. ClubMeetups' description says to pass
  `$upcomingOnly: false` for past meetups; the raw query did not use the operation, and the schema's own
  description of `meetups` says nothing about it.
- **Over-fetching priced by the cost function, not by the data.** q02, q06, q07 and q10 list the catalogue with
  `books(first: 50)`. Shelfie has 10 books, but cost is a prediction from arguments, so each costs 150 to 250.
  The reference queries used `first: 100` and cost twice as much for the same data. There is no author filter
  (q02) and no "has reviews" or ordering argument (q06, q07, q10), so listing everything is the only way: the
  cost is the schema's, not the model's.
- **Answered is not the same as right.** q01's response holds every club's name and rating; the scorer counts
  it as answered because "Slow Burn Mysteries" is in it, but the model still has to filter by `PUBLIC` and break
  the tie with Booker Shortlist Survivors (both 4.5, one private). q03 and q04 list all clubs rather than one.

## What the descriptions changed

Without a second run this is reasoning, not measurement. The descriptions that should matter most are the ones
that state what the schema hides: ClubMeetups' default, FindBooks' missing author filter ("list the catalogue and
filter yourself"), and Search's "the quickest way to turn a name into an id" (q05 and q08 used it). The sample
run shows the limit: a model that writes raw queries can ignore an operation's description entirely, which is
one argument for lab 18.2's design, where the agent can only call the described operations.

## The riskiest query

None of the ten writes, but `{ books(first: 50) { ... } }` is the pattern to watch: an agent that "lists
everything and filters" turns every question into a full scan, and on a real catalogue `first` has no cap on this
server. Run as raw GraphQL it should meet lab 18.3's cost cap; better, the agent never sends raw GraphQL and gets
a `FindBooks` with a small default page instead. A raw tool would also let the model write a mutation for a
question it misread; lab 18.3 refuses those unless a human approves.
