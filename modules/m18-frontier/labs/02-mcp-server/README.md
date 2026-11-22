# Lab 18.2: An MCP server in Java

Lecture: [18.2 An MCP server in Java](../../lectures/18.2-mcp-server.md). Grading: auto + rubric. Needs the
reference server.

The [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) is how assistants such as Claude, ChatGPT and
IDE agents call outside tools: JSON-RPC over stdio or HTTP, with `tools/list` to discover tools and `tools/call`
to run one. The starter is Shelfie's MCP server on Spring AI 2.0.2 (`spring-ai-starter-mcp-server-webmvc`,
Streamable HTTP at `/mcp`) with the quickest design there is: one `run_graphql` tool that runs whatever the
agent writes.

Replace it with **eight curated read tools**, one per read operation from lab 18.1 (already in
`src/main/resources/graphql-documents/`, with your descriptions):

| Tool | Operation | Parameters (required in bold) |
| --- | --- | --- |
| `find_books` | FindBooks | `titleContains`, `first` |
| `book_details` | BookDetails | **`id`** |
| `list_clubs` | ListClubs | none |
| `club_details` | ClubDetails | **`id`** |
| `club_meetups` | ClubMeetups | **`clubId`**, `upcomingOnly` |
| `reader_profile` | ReaderProfile | **`id`** |
| `reader_reviews` | ReaderReviews | **`id`** |
| `search` | Search | **`text`** |

- Each tool is a Spring AI `@Tool` method with a description an agent can act on (12 words or more) and typed
  `@ToolParam`s; it runs its operation by name with `GraphQlClient.documentName(...)` and returns the `data`.
- The operation files keep their descriptions. Shelfie's graphql-java 26.1 rejects them, so strip them before the
  document is sent: a `DocumentSource` that wraps Spring's `ResourceDocumentSource` is the natural place.
- No tool takes GraphQL from the agent. The two mutations wait for lab 18.3's safety policy.

Then connect a real assistant and ask it **"Which public club is reading the highest-rated book right now?"**
(`CONNECT.md`). The server runs inside the course container; publish its port so a client on your machine can
reach it:

```bash
bin/course start m18/02
cd workspace/m18-frontier/02-mcp-server
COURSE_PUBLISH=4000 bin/course shell -c 'mvn -q package && java -jar target/shelfie-mcp.jar'
# another terminal, e.g. Claude Code (from its documentation; not run when this lab was written):
claude mcp add --transport http shelfie http://localhost:4000/mcp
# or the course's own client, inside the image:
bin/course shell -c 'source /course/tools/testlib.sh; source /course/modules/m18-frontier/contract/mcp.sh;
  mcp_init http://localhost:4000/mcp && mcp_tools | jq ".[].name" && mcp_text list_clubs'
bin/course test m18/02
```

## Done when

`bin/course test m18/02` passes: exactly the eight tools, none taking GraphQL, each answering what its operation
answers when Shelfie runs it directly; and `CONNECT.md` passes the rubric (`bin/course grade m18/02`).
