# Lab 1.4 — Change impact

Product asks: **the club page must show each member's reading progress on the current book** (pages
read, e.g. "Asha Rao — 212 / 304"). Using the reference server's three implementations as they are
today, answer for each style.

## REST
- Which files change, and roughly what goes in them?
- New endpoint, new field on an existing resource, or both? What does that do to the request count?
- Does any existing client break, or download more than before?

## gRPC
- Same three questions. Do you change `GetClubPage`, the resource RPCs, or both?
- What happens to clients compiled against the old `.proto`?

## GraphQL
- Same three questions. Where does the new field go in `schema.graphqls`, and what new resolver is needed?
- Which clients see the change, and which do not?

## Verdict
In which style did the change cost least, for whom? Where did the cost move instead?
