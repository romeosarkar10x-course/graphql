# Connecting an assistant

Reference notes. The test uses the course's own client (`contract/mcp.sh`, curl and jq over Streamable HTTP). A
real assistant was connected on 2026-11-24; the transcripts (stream-json) are in
`_workbench/2026-11-24-finish/mcp-connect/`. A good answer has a real transcript like this one.

## Setup

What a reproducible setup states: the client and its version (Claude Code, Claude Desktop, an IDE agent, the MCP
Inspector), how it was pointed at the server (`claude mcp add --transport http shelfie http://localhost:4000/mcp`,
or a JSON config with `"type": "http"` and the URL), and that the server ran with `COURSE_PUBLISH=4000` so the
host could reach the container.

Measured setup: Claude Code 2.1.330 on the host, the server started with
`COURSE_PUBLISH=4000 bin/course shell -c 'java -jar target/shelfie-mcp.jar'`, and a JSON config instead of
`claude mcp add` (so nothing changes in the user's settings):

```bash
echo '{"mcpServers":{"shelfie":{"type":"http","url":"http://localhost:4000/mcp"}}}' > mcp.json
claude -p "Which public club is reading the highest-rated book right now? Use the shelfie tools." \
  --mcp-config mcp.json --strict-mcp-config --tools "" --allowedTools mcp__shelfie --output-format stream-json --verbose
```

The client reported `shelfie` as connected with eight tools (`book_details`, `club_details`, `club_meetups`,
`find_books`, `list_clubs`, `reader_profile`, `reader_reviews`, `search`).

## The question

"Which public club is reading the highest-rated book right now?" Expected path: `list_clubs` once (no
arguments), then reasoning over the result: The Midnight Readers' book has 4.2, Slow Burn Mysteries' and Booker
Shortlist Survivors' have 4.5 each, and Booker Shortlist Survivors is `PRIVATE`. The right answer is Slow Burn
Mysteries. Two traps an assistant can fall into: naming both 4.5 clubs (the tie) and ignoring visibility. Some
assistants call `club_details` for each club first; that is three extra calls, not an error.

What happened: one call, `list_clubs {}`, then "**Slow Burn Mysteries** (c2) ... *The Name of the Rose*, with a
mean rating of 4.5. ... Booker Shortlist Survivors is reading a 4.5-rated book too, but that club is private, so
it doesn't count." Right, both traps avoided, 2 turns, 3.2 s.

## A question no operation covers

"Which reader has written the most reviews?" What happened: 20 turns. `list_clubs`, `search {"text": "a"}`, then
`reader_reviews` for `u1` to `u12` and `reader_profile` for `u12` to `u16`, guessing ids until four in a row did
not exist. The answer was right (Asha Rao, 3; Chloe Martin and Jonas Berg, 2: checked against the reference
data), and the assistant said how it could be wrong: "If any readers have ids above u16, I wouldn't have seen
them." That is the price of curated tools, measured: 18 calls where one raw query would do, and an enumeration of
ids that a real API should rate-limit (lab 18.3).

## Raw tool or curated tools

With `run_graphql`, an assistant usually writes almost the same `clubs { ... }` query, so the answer is the same.
The difference is what it could send: any mutation (`deleteReview`, `leaveClub`) with whatever user header the
server forwards, a fan-out like `clubs { members { clubs { members { ... } } } }` with no cost limit, or
`__schema` introspection of every type and field. With curated tools the agent can only run the ten known,
priced documents; it cannot select a field the operations do not select. The price: a question no operation
covers ("which reader has written the most reviews?") needs `search` and `reader_reviews` per reader, or a new
described operation, added and reviewed like any other API change.
