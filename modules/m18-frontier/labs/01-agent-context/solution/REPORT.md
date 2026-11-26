# Report: an LLM against Shelfie

Reference notes. Two fair runs were made on 2026-11-24 (`_workbench/2026-11-24-finish/llm-1801/`): Claude Code
2.1.330 in print mode, no tools, no feedback, a fresh session per run, given Shelfie's schema
(`/graphql/schema`) and the ten questions, then in the second run also the ten described operations. The
sample in `llm/` is the earlier, unfair one: written by the authoring model with the data already seen.

## The runs

| Run | Model | Valid | Answered | Total cost |
| --- | --- | --- | --- | --- |
| Schema only | Claude Code 2.1.330 (print mode), one query per question, no execution feedback | 10 | 10 | 21,028 |
| Schema and the ten described operations | same | 10 | 10 | 20,045 |
| Authoring sample (`llm/`, not fair: data seen) | Claude Opus 5.5 while building the lab | 10 | 9 | 835 |
| The reference queries (`reference/`, hand-tuned) | — | 10 | 10 | 1,527 |

Scores from `agent score` (`scores.txt` there). Both fair runs found the hidden default the sample missed
(q09: `meetups(upcomingOnly: false)`, from the schema alone), so the descriptions did not change the score. What
they changed was the route: with the operations in front of it, the model reached people and clubs through
`search` (q05, q08, q09) as the described operations do, instead of scanning lists. What they did not change is
the price: both runs asked for `books(first: 1000)` wherever the question needed the catalogue, 20 times the
schema's default, and each such query costs 3,000 to 7,000 by Module 10's function. The data is ten books, so the
answers came back; under the course's cost limits most of these queries would be refused. A model that cannot
see the data plays safe by asking for everything; the schema has no total count or filter for it to aim at.

## Failed and costly queries

In the authoring sample (`llm/`):

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

Measured, two fair runs: not the score (10 of 10 both times) and hardly the cost (21,028 against 20,045). They
changed the route: with the operations, the model turned names into ids with `search`, as Search's description
says ("the quickest way to turn a name into an id"), where the schema-only run scanned `books(first: 1000)` and
every club. They did not stop the over-fetching: FindBooks' description says there is no author filter, and the
model answered that by listing everything at `first: 1000`. A model that writes raw queries can read a
description and still ignore its advice on size, which is one argument for lab 18.2's design, where the agent
can only call the described operations with their own variables.

## The riskiest query

None of the ten writes, but `{ books(first: 1000) { ... } }` (both fair runs; `first: 50` in the sample) is
the pattern to watch: an agent that "lists
everything and filters" turns every question into a full scan, and on a real catalogue `first` has no cap on this
server. Run as raw GraphQL it should meet lab 18.3's cost cap; better, the agent never sends raw GraphQL and gets
a `FindBooks` with a small default page instead. A raw tool would also let the model write a mutation for a
question it misread; lab 18.3 refuses those unless a human approves.
