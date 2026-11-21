# 2026-11-21 Module 7 probe

- `rfc-*.html`, `semnull.txt`: rfcs.graphql.org pages fetched 2026-11-21. Stages on that day:
  ObjectIdentification RFC 0 (Lenz Weber-Tronic, Apollo; proposes `__id`), SemanticNullability RFC 0
  (document lists `@semanticNonNull(levels: [Int!]! = [0]) on FIELD_DEFINITION` among options),
  #1163 error behaviors RFC 1 (champion benjie; onError PROPAGATE | NULL | ABORT; WG 2026-08-06: PROPAGATE
  stays the default forever), #1231 RFC 1 (advanced 2026-07-02), #997 RFC 1 (@deprecated on objects).
- `Probe.java` / `probe.txt`: graphql-java 26.1. `@experimental_disableErrorPropagation` is enabled by
  default and keeps `{club {name, book {title, rating: null}}}` where the default nulls the whole club.
  SchemaDiff levels: remove field BREAKING/MISSING, add required arg BREAKING/STRICTER, arg `Int`->`Int!`
  BREAKING/STRICTER, add enum value DANGEROUS/ADDITION, deprecate DANGEROUS/DEPRECATION_ADDED, add field
  INFO/ADDITION, add optional arg: no event, output `String`->`String!`: no event.
- `old.graphql`, `new.graphql`, `inspector-probe.txt`: graphql-inspector 7.0.2 (docker/inspector.Dockerfile)
  on the same kind of changes: deprecation and output `E`->`E!` are safe (✔), enum value added is
  dangerous (⚠), required arg added is breaking (✖). Differs from SchemaDiff on deprecation.
- `anilist-*.json`: live AniList introspection, 2026-11-21. `Mutation.UpdateUser` has 19 arguments
  (about, titleLanguage, displayAdultContent, ... disabledListActivity); `Query.Media` has 75 arguments
  (`id_not_in`, `startDate_greater`, ...); root fields are PascalCase; a `Json` scalar with an empty
  description types `User.bans`, `MediaList.customLists`, `MediaList.advancedScores` and more;
  `Media.updatedAt: Int`.
- GitHub snapshot (`tools/schemas/github-2026-11-21.graphql`): 570 `clientMutationId` lines, 73
  `databaseId: Int` fields ("Identifies the primary key from the database"; deprecated for
  `fullDatabaseId: BigInt`, removal 2027-01-01), `UpdateRepositoryInput` with 14 fields and 6
  `has*Enabled` booleans, `EnterpriseOwnerInfo.domains(isApproved: Boolean = null, isVerified: Boolean = null)`,
  `Issue.closedByPullRequestsReferences(includeClosedPrs: Boolean = false)`, `scalar CustomPropertyValue`
  ("either a string or an array of strings").
