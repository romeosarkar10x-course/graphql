# Game 4b — Design critique cards

**Lecture:** after [4.3 Errors as data](../../lectures/4.3-errors-as-data.md) · **Grading:** auto + rubric · **Needs:** nothing · **Kind:** game

Eight real mutation signatures, copied from the public schemas on 2026-11-21 (GitHub from
`tools/schemas/github-2026-11-21.graphql`; AniList by introspection of `https://graphql.anilist.co`;
Shelfie from the reference server). Rate each on five criteria, 0 to 2, and justify every score in a
sentence.

## The criteria

| Criterion | 0 | 1 | 2 |
|---|---|---|---|
| **input** | many loose arguments | some structure | one `input` object, required fields non-null |
| **payload** | returns a bare scalar or entity | returns the entity | dedicated payload with what clients need to update caches |
| **errors** | failures only as top-level errors | partial | typed domain errors (userErrors or result types) |
| **retry** | unsafe to retry, no help | safe by nature or partly | idempotent by design, or concurrency control offered |
| **naming** | unclear or inconsistent | acceptable | verb-first, says what happens |

## The cards

```graphql
# 1. GitHub
addStar(input: AddStarInput!): AddStarPayload
input AddStarInput { clientMutationId: String  starrableId: ID! }
type AddStarPayload { clientMutationId: String  starrable: Starrable }

# 2. GitHub
createIssue(input: CreateIssueInput!): CreateIssuePayload
input CreateIssueInput { repositoryId: ID!  title: String!  body: String  assigneeIds: [ID!]  labelIds: [ID!]
                         milestoneId: ID  projectIds: [ID!]  issueTemplate: String  clientMutationId: String  ... }
type CreateIssuePayload { clientMutationId: String  issue: Issue }

# 3. GitHub
mergePullRequest(input: MergePullRequestInput!): MergePullRequestPayload
input MergePullRequestInput { pullRequestId: ID!  expectedHeadOid: GitObjectID  mergeMethod: PullRequestMergeMethod = MERGE
                              commitHeadline: String  commitBody: String  authorEmail: String  clientMutationId: String }
type MergePullRequestPayload { actor: Actor  clientMutationId: String  pullRequest: PullRequest }

# 4. GitHub
deleteIssue(input: DeleteIssueInput!): DeleteIssuePayload
input DeleteIssueInput { issueId: ID!  clientMutationId: String }
type DeleteIssuePayload { clientMutationId: String  repository: Repository }

# 5. AniList
SaveMediaListEntry(id: Int, mediaId: Int, status: MediaListStatus, score: Float, scoreRaw: Int, progress: Int,
                   progressVolumes: Int, repeat: Int, priority: Int, private: Boolean, notes: String,
                   hiddenFromStatusLists: Boolean, customLists: [String], advancedScores: [Float],
                   startedAt: FuzzyDateInput, completedAt: FuzzyDateInput): MediaList

# 6. AniList
ToggleFavourite(animeId: Int, mangaId: Int, characterId: Int, staffId: Int, studioId: Int): Favourites

# 7. AniList
DeleteMediaListEntry(id: Int): Deleted

# 8. Shelfie (reference server)
createReviewTyped(input: CreateReviewInput!): CreateReviewResult!
union CreateReviewResult = ReviewCreated | ValidationFailed | NotAllowed
```

## Do

1. Fill `cards.json`: for each card, `input`, `payload`, `errors`, `retry`, `naming` as 0, 1 or 2, and
   `why`: one sentence per criterion (an object with the same five keys).
2. Pick the best and the worst card and write `verdict.md`: what would you change in the worst one
   (rewrite its signature), and what would you copy from the best one.

The test checks the cards are complete and shows how far your scores are from the reference scores
(after you pass). Disagreeing is fine; the rubric grades the reasons.

## Group variant

Everyone rates alone (15 minutes). Reveal card by card; for each criterion where scores differ by 2,
the high and the low scorer each get one minute. The group can change the reference score by
majority: add your verdict to `verdict.md`.
