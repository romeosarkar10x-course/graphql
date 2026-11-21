# Lab 5.5 notes

## The slow fields

Course image, 2026-11-21 (`report` on `club-pages.graphql`):

| Coordinate | max ms | Called how many times in the query? |
|---|---|---|
| Book.averageRating | 121 | 3 (once per club's current book) |
| Club.meetups | 77 | 3 |
| Query.clubs | 62 | 1 |

## Questions

1. With `beginFieldExecution` (probe: `_workbench/2026-11-21-m05-probe/timing-field-execution.txt`)
   `Query.clubs` jumps to 808 ms and `Club.currentBook` (127 ms) joins: execution time includes completing
   the field's whole subtree, so every ancestor of a slow field looks slow too. To find what to optimise,
   use fetch time: it points at the field that is itself slow. Execution time answers a different
   question: which subtree costs the most.
2. A static map is shared by all requests: concurrent requests would overwrite each other's maxima and
   counts, report each other's slow fields, and the map would grow forever. `createStateAsync` gives one
   state object per execution, handed back to every hook of that execution.
3. Fetchers are synchronous, so the clubs' fields run one after another: 50 x (120 + 75) ms is about
   9.8 s plus 60 ms for the list. Now: make the slow fetchers async on an executor (5.3) so the 50 calls
   overlap, or cache the ratings. Module 8: batch with a DataLoader so the 50 `averageRating` calls become
   one call to the ratings service.
4. Not by default: any client could read them, they leak backend structure and cost bytes. Gate them
   behind a debug flag or an internal-caller check; send per-coordinate histograms to metrics (Micrometer,
   OpenTelemetry spans per field, Module 15) where only operators see them.
