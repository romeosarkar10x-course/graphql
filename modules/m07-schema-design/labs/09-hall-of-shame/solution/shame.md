# Hall of shame (reference ranking; a different ranking with good reasons scores the same)

## 1. Rank the cards

| Rank | Card | Client cost | Lock-in | Case for convincing? | One-line verdict |
|---|---|---|---|---|---|
| 1 | C `Json` | 5 | 4 | Partly: custom lists are user-shaped, but `advancedScores` is not | No validation, no docs, no codegen: every client hand-parses |
| 2 | B `Media` 75 args | 4 | 4 | Partly | A filter language encoded as argument names; an input type with typed operators says the same in 15 fields |
| 3 | A `UpdateUser` 19 args | 4 | 3 | No | Unrelated settings in one call; partial failures have nowhere to go |
| 4 | E `databaseId` | 3 | 5 | Yes for the deprecation | Leaked the key and its size; the fix costs a two-year migration |
| 5 | H `CustomPropertyValue` | 3 | 4 | Yes, before `@oneOf` | A union clients must sniff at run time |
| 6 | D `updatedAt: Int` | 2 | 3 | Partly | Seconds or milliseconds? A `DateTime` scalar answers it in the type |
| 7 | F tri-state booleans | 2 | 2 | Yes | Three states are real; an enum `DomainFilter { APPROVED UNAPPROVED ANY }` reads better |
| 8 | G `clientMutationId` | 1 | 5 | Yes | Harmless noise; the price of being early |
| 9 | I `Repository` 147 fields | 1 | 2 | Yes | Big because the domain is big; clients select what they need |

## 2. Fix your worst three

**C: replace `Json` with types.**

```graphql
"A user's custom list in one of their media lists."
type CustomList { name: String! enabled: Boolean! }
type MediaList { id: ID! customLists: [CustomList!]! advancedScores: [AdvancedScore!]! }
"A score for one aspect (story, art...)."
type AdvancedScore { aspect: String! score: Float! }
```

Migration: add `customListsTyped` and `advancedScoresTyped` (or a `v2` sub-object) next to the `Json`
fields, deprecate the old ones with a date, watch usage, remove after a full client cycle. The clean names
come back later via a rename-by-alias period (add `customLists2`, move clients, then swap): naming costs
two deprecation cycles, which is why getting it right first time matters.

**B: one typed filter input.**

```graphql
# excerpt: FuzzyDateFilter and MediaFormatFilter follow the IdFilter pattern
input MediaFilter {
  id: IdFilter
  startDate: FuzzyDateFilter
  episodes: IntRange
  format: MediaFormatFilter
}
"Each operator once: in, notIn, eq, not."
input IdFilter { eq: Int not: Int in: [Int!] notIn: [Int!] }
input IntRange { min: Int max: Int }
```

`Media(filter: MediaFilter)` added alongside; the 74 flat arguments deprecated (arguments can be
deprecated since the September 2025 spec); usage per argument decides the removal order.

**A: split by intent.** `updateProfile(input: {about, profileColor})`, `setNotificationPreferences(input:
{airingNotifications, ...})`, `setListPreferences(input: {scoreFormat, rowOrder, animeListOptions,
mangaListOptions})`, `setContentPreferences(input: {displayAdultContent, titleLanguage, staffNameLanguage})`,
each with a payload that returns the changed settings object. `UpdateUser` stays, deprecated, and is
implemented by calling the four new mutations.

## 3. Your vote (group) or your pitch (solo)

Card C. A `Json` scalar opts out of everything GraphQL is for: the type system, validation,
introspection, tooling, generated clients. Every other card makes clients' lives harder; this one makes the
schema stop describing the data at all, and once clients parse the blob by hand, its shape is frozen
anyway, just without documentation.
