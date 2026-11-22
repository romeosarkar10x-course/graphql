# Hints: 17.8 GraphQL over HTTP

1. Run the audit before you change anything and sort its lines by level: errors first. Each line has an id and a
   one-line description; graphql-http's source (`/course/.cache/m17-http-audit/node_modules/graphql-http/lib/audits/server.js`) has the request
   each audit sends.
2. The three MUST errors are one cause: what a JSON `null` means for an optional parameter. Look at how the server
   reads `variables`, `operationName` and `extensions` from the body.
3. For each remaining warning, find the section of the edition about the same situation (legacy clients, status
   codes, Accept) and compare what it says with what the audit wants. Quote both in `AUDIT.md`.
