# Lab 10.5: Trusted documents

Lecture: [10.5 Trusted documents](../../lectures/10.5-trusted-documents.md). Grading: auto + rubric. Needs nothing.

The starter is lab 10.4's solution plus Shelfie's web client, as far as the server sees it:
`client/operations/` holds its 8 operations. Make the server run those and nothing else.

1. **The client build**: `client/BuildManifest.java` writes `src/main/resources/trusted-documents.json` in
   Apollo's persisted query manifest format. Each id is the SHA-256 of the operation's text, trimmed.
2. **The server**: a `PreparsedDocumentProvider` (registered with a `GraphQlSourceBuilderCustomizer`) that
   - runs the manifest's body for `extensions.persistedQuery.sha256Hash`,
   - runs full text whose hash is in the manifest (old clients),
   - refuses a known id sent with different text, an unknown id (even with matching text: APQ is not
     trust), and every other document, with `extensions.code` `NOT_TRUSTED`,
   - lets ad-hoc documents through only with `shelfie.trusted-documents.enforce=false`.
3. **Introspection off**: `spring.graphql.schema.introspection.enabled: false`, in development too.
4. **The experiment**: replay a captured query, edited, against the hardened server; record both results.

A request with only an id reaches your provider with graphql-java's placeholder text
(`PersistedQuerySupport.PERSISTED_QUERY_MARKER`), not an empty string.

```bash
bin/course start m10/05
bin/course java client/BuildManifest.java client/operations src/main/resources/trusted-documents.json
bin/course test m10/05
```

## Done when

`bin/course test m10/05` passes (the manifest build, 9 Hurl checks, development mode, introspection off)
and `notes.md` passes the rubric (`bin/course grade m10/05`).
