# Hints: 16.3 governance

1. Split the schema at each `type` keyword. Keep each type's description with it, and name each file after its
   type. `bin/course design diff old.graphqls src/main/resources/graphql/types` accepts a directory, so you can
   check the split on its own first: before you remove anything it must say `verdict: SAFE`.
2. A CODEOWNERS line is a path pattern followed by one or more owners. Later lines override earlier ones, so
   put a catch-all first. Paths starting with `/` are relative to the repository root, the lab folder here.
3. Before you remove a field, count its operations per client version in a window and find the last time it was
   used. A deprecated field can still be in use. If a supported client still sends it, it stays, and the
   changelog says until when. The reports carry each operation's timestamp and `metadata.client`.
