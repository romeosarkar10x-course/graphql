# Connecting an assistant

Reference notes. No assistant was connected when this lab was written; the protocol was exercised with the
course's own client (`contract/mcp.sh`, curl and jq over Streamable HTTP), which is what the test uses too
(`_workbench/2026-11-23-m18-probe/mcp-server/probe2.txt`). A good answer has a real transcript.

## Setup

What a reproducible setup states: the client and its version (Claude Code, Claude Desktop, an IDE agent, the MCP
Inspector), how it was pointed at the server (`claude mcp add --transport http shelfie http://localhost:4000/mcp`,
or a JSON config with `"type": "http"` and the URL), and that the server ran with `COURSE_PUBLISH=4000` so the
host could reach the container.

## The question

"Which public club is reading the highest-rated book right now?" Expected path: `list_clubs` once (no
arguments), then reasoning over the result: The Midnight Readers' book has 4.2, Slow Burn Mysteries' and Booker
Shortlist Survivors' have 4.5 each, and Booker Shortlist Survivors is `PRIVATE`. The right answer is Slow Burn
Mysteries. Two traps an assistant can fall into: naming both 4.5 clubs (the tie) and ignoring visibility. Some
assistants call `club_details` for each club first; that is three extra calls, not an error.

## Raw tool or curated tools

With `run_graphql`, an assistant usually writes almost the same `clubs { ... }` query, so the answer is the same.
The difference is what it could send: any mutation (`deleteReview`, `leaveClub`) with whatever user header the
server forwards, a fan-out like `clubs { members { clubs { members { ... } } } }` with no cost limit, or
`__schema` introspection of every type and field. With curated tools the agent can only run the ten known,
priced documents; it cannot select a field the operations do not select. The price: a question no operation
covers ("which reader has written the most reviews?") needs `search` and `reader_reviews` per reader, or a new
described operation, added and reviewed like any other API change.
