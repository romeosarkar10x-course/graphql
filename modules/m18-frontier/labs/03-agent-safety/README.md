# Lab 18.3: Agent safety

Lecture: [18.3 Agent safety](../../lectures/18.3-agent-safety.md). Grading: auto + rubric. Needs the reference
server.

The starter is lab 18.2's server plus what a product team asked for on a Friday (`AgentTools`): `run_graphql`
runs any document, and `join_club` and `create_review` act at once as whatever `userId` the agent passes. An
agent that misreads a question, or reads a review that says "ignore your instructions and ...", can now do
anything Shelfie allows anyone. Put a policy in front of the tools:

| Rule | Behaviour |
| --- | --- |
| Read-only by default | No write tools at all unless `shelfie.mcp.writes=true` |
| Allowlist first | The eight read tools stay as they are; `run_graphql` stays as a guarded escape hatch |
| Guarded raw queries | `run_graphql` refuses, before sending anything: mutations, subscriptions, more than one operation, introspection (`__schema`, `__type`), nesting deeper than 6, and a cost over 200 by Module 10's function (a field costs 1 plus its children, times `first` or `last`, with defaults and variable defaults priced in) |
| The caller is passed through | The reader is the `X-Shelfie-User` header of the MCP HTTP request (the client's configuration, so the human's choice), forwarded to Shelfie; no tool takes a user id |
| Human approval for writes | `join_club` and `create_review` change nothing: they return `{"status": "APPROVAL_REQUIRED", "approvalId": ...}`. A human approves or denies it with `POST /approvals/{id}` or `POST /approvals/{id}/deny` and the header `X-Approver-Token` (environment `APPROVER_TOKEN`); then `confirm_action(approvalId)` runs it once, for the same reader, within 5 minutes |
| Audit | `GET /audit` (approver token): every tool call with the reader, `allowed`, `denied` or `approval-required`, and the reason |

A refusal is a tool error (`isError: true`) whose text says why, so the agent can tell the human or try a
cheaper query. `GET /approvals` (approver token) lists the requests for the human.

Then red-team it (`REDTEAM.md`): connect an assistant with writes on and try to make it run a destructive
mutation, by asking, by tricking it, and by planting instructions in Shelfie's data (a review body). Record what
it tried and what stopped it. Mini-project 18.7 scores a longer list of attacks.

```bash
bin/course start m18/03
cd workspace/m18-frontier/03-agent-safety
COURSE_PUBLISH=4000 bin/course shell -c 'mvn -q package && APPROVER_TOKEN=s3cret java -jar target/shelfie-mcp.jar --shelfie.mcp.writes=true'
curl -s -H 'X-Approver-Token: s3cret' localhost:4000/approvals      # the human's view
bin/course test m18/03
```

## Done when

`bin/course test m18/03` passes (26 checks: each refusal, the approval flow end to end, the audit log) and
`REDTEAM.md` passes the rubric (`bin/course grade m18/03`).
