# Lab 6.3 notes

## The starter's report

```
Unmapped fields: {}
Unmapped registrations: {}
Unmapped arguments: {}
Field nullness errors: {}
Argument nullness errors: {ShelfieController#reviews[2 args]=[int last should be NULLABLE], ShelfieController#members[2 args]=[int first should be NULLABLE], ShelfieController#meetups[2 args]=[boolean upcomingOnly should be NULLABLE], ShelfieController#books[2 args]=[int first should be NULLABLE]}
Skipped types: []
```

## Questions

1. `books(first: Int = 20)`: the type is nullable `Int`; the default only applies when the argument is
   left out. A client may send `first: null` explicitly, and Spring cannot put null into `int`.
   `{ books(first: null) { id } }` against the starter returns `INTERNAL_ERROR` at path `books`, and since
   `books` is `[Book!]!`, `data` is `null` for the whole response. Game 5b's B5, caught at startup.
2. The renamed method: `Club.memberCount` became unmapped, and `PropertyDataFetcher` finds no
   `memberCount` component on the record, so every club page would show `memberCount: null` in a non-null
   field: an error, and the whole club nulled. The nullability lie (`currentBook: Book!` with a nullable
   method) would break only for a club with no current book, which is the worst kind: rare and in
   production. `tagline` would simply be null forever, quietly.
3. `@NullMarked` on the package makes every unannotated type non-null, so only the `@Nullable` ones are
   nullable and the inspector can compare each method with the schema. `@Nullable` is a type-use
   annotation: on a nested type it must sit next to the simple name, `Data.@Nullable Club`, because
   `@Nullable Data.Club` would annotate the outer name `Data`.
4. Fail in production too. A mismatch is a bug that will surface as nulls and errors for users; failing
   startup turns it into a deployment that never receives traffic, and rolling deployments keep the old
   version serving. The cost is that an unused unmapped field blocks a release, which is the right
   pressure: map it or delete it.
