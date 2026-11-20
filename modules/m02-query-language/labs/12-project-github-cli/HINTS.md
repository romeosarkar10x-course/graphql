# Hints, GitHub profile CLI

1. Build the query in GitHub's explorer (https://docs.github.com/en/graphql/overview/explorer) first. Counts live in connections: `followers { totalCount }`. Owned public repositories: `repositories(ownerAffiliations: OWNER, privacy: PUBLIC) { totalCount }`. Contributions: `contributionsCollection { contributionCalendar { totalContributions } }`.
2. `pinnedItems(first: $pinned, types: REPOSITORY)` returns a union (`PinnableItem`), so its nodes need `... on Repository { ... }`. picocli fills annotated fields before `call()` runs; `--endpoint` is already declared for you.
3. Jackson's `JsonNode.path("x")` never returns null (a missing node instead), and `asString("")` gives a default for null or missing values. Return the exit code from `call()`; `main` passes it to `System.exit`.
