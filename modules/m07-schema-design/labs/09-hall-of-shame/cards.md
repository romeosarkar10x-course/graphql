# Hall of shame: the cards

Nine excerpts from two large, successful public APIs: AniList (live introspection, 2026-11-21) and
GitHub (the schema snapshot in `tools/schemas/github-2026-11-21.graphql`). Evidence:
`_workbench/2026-11-21-m07-probe/`. These APIs serve millions of requests; each card has a **case for**,
because real anti-patterns usually had a reason. Re-check the counts each term (TODO.md).

| Card | API | Excerpt | Pattern |
|---|---|---|---|
| A | AniList | `Mutation.UpdateUser` takes **19 arguments**: `about`, `titleLanguage`, `displayAdultContent`, `airingNotifications`, `scoreFormat`, `rowOrder`, `profileColor`, `timezone`, `animeListOptions`, ... `disabledListActivity` | Kitchen-sink update; arguments instead of one input; PascalCase field name |
| B | AniList | `Query.Media` takes **75 arguments**: `id`, `id_not`, `id_in`, `id_not_in`, `startDate_greater`, `startDate_lesser`, `startDate_like`, `episodes_greater`, ... `sort` | Filter grammar flattened into arguments; snake_case suffixes |
| C | AniList | `scalar Json` (empty description) types `User.bans`, `MediaList.customLists`, `MediaList.advancedScores`, `Mutation.UpdateAniChartSettings` and more | Untyped escape hatch |
| D | AniList | `Media.updatedAt: Int` | A timestamp as a number with no unit in the type |
| E | GitHub | `databaseId: Int` on 73 types, described as "Identifies the primary key from the database"; deprecated: "will be removed because it does not support 64-bit signed integer identifiers. Use `fullDatabaseId` instead. Removal on 2027-01-01 UTC." | Database key on the surface; and a 32-bit `Int` that ran out |
| F | GitHub | `EnterpriseOwnerInfo.domains(isApproved: Boolean = null, isVerified: Boolean = null, ...)` | Boolean flag arguments, three-state (true, false, null = don't filter) |
| G | GitHub | `clientMutationId: String` on 253 of 298 `*Input` types (e.g. `UpdateRepositoryInput`), echoed in payloads | Relay Classic legacy kept for compatibility |
| H | GitHub | `scalar CustomPropertyValue`: "A custom property value can be either a string or an array of strings." | A union hidden inside a scalar |
| I | GitHub | `type Repository` has **147 fields** | A god type |

## The case for each

- A: one settings screen, one save button, one mutation; AniList predates the input-object convention.
- B: power users build arbitrary searches; one field with every filter is easy to document and cache.
- C: user-defined structures (custom lists) have no fixed shape.
- D: Unix seconds are unambiguous and small; no custom scalar for clients to configure.
- E: integrations needed REST-compatible ids; deprecating with a date is exactly the lifecycle of lecture 7.6.
- F: the null default means "no filter", so the three states are real.
- G: removing it would break every client that sends it: the cost of an early convention.
- H: GraphQL has no input unions (`@oneOf` arrived in 2025), and the value is both input and output.
- I: a repository really is the centre of GitHub; splitting it would only move the fields.
