# Game 7b — Hall of shame

**Lecture:** [7.7 Anti-patterns and boundaries](../../lectures/7.7-anti-patterns.md) · **Grading:** rubric · **Needs:** nothing

Lab 7.7's bad schema was made up. [`cards.md`](cards.md) is not: nine excerpts from AniList and GitHub,
two APIs that serve millions of requests, with the numbers checked on 2026-11-21. Each card has a
"case for": the reason it probably looked fine at the time. Rank them, fix the worst, and argue.

## Solo

1. `shame.md` §1: rank all nine, worst first, scoring client cost and lock-in 1 to 5. Read each card's
   case for and say whether it convinces you.
2. §2: write the fix for your worst three as SDL, plus the migration (add, deprecate, measure, remove).
   Check your SDL parses: `../../../bin/course design lint fix.graphqls --rules naming` on a scratch file.
3. §3: your pitch for the wall, in under 150 words.

You can look at the live AniList schema yourself (`bin/course shell`, then curl `https://graphql.anilist.co`
with an introspection query) or grep `tools/schemas/github-2026-11-21.graphql` for more examples. Bring
one more card if you find a better one.

## Group (3 to 8 players)

1. Each player ranks the cards alone (10 minutes), then picks **one** card, not the same as anyone else's
   (draw lots on clashes).
2. Each presents their card in 3 minutes: the excerpt, the cost to clients, the case for, the fix.
3. Everyone votes for the card that goes "on the wall" (no voting for your own). Then a second vote:
   whose fix is best?
4. Debrief: which "case for" was strongest, and which pattern does the group's own codebase have?

## Done when

`bin/course grade m07/09` gives 7/10 or more on `shame.md`.
