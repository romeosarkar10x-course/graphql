# Web clients: notes

Reference notes (grader only), from `bin/course web run measure` on 2026-11-22 (Apollo Client 4.2.12,
Relay 20.1.1, React 19.2.9, jsdom 27.4.0; `_workbench/2026-11-22-m12-probe/web-renders.txt`).

## The numbers

```
Apollo Client, props
  first load:     ClubCard 2, BookPanel 1, MemberRow 8
  after addReview: ClubCard 1, BookPanel 1, MemberRow 8   ("The Left Hand of Darkness: 4.2" -> "The Left Hand of Darkness: 3.67", 2 requests)
Apollo Client, props + React.memo
  first load:     ClubCard 2, BookPanel 1, MemberRow 8
  after addReview: ClubCard 1, BookPanel 1, MemberRow 0
Apollo Client, data masking
  first load:     ClubCard 2, BookPanel 1, MemberRow 8
  after addReview: ClubCard 0, BookPanel 1, MemberRow 0
Relay
  first load:     ClubCard 3, BookPanel 1, MemberRow 8
  after addReview: ClubCard 0, BookPanel 1, MemberRow 0
```

All four showed the new average (4.2 to 3.67) with 2 requests: the query and the mutation. No refetch:
each normalised cache merged `Book:b1.averageRating` (Relay: record `b1`) from the mutation's response.

## Why each count is what it is

- **Props:** `useQuery` in `ClubCard` watches the whole query; any change to any field in it re-renders
  `ClubCard`, and React re-renders every child it returns: 1 + 1 + 8.
- **Props + memo:** Apollo keeps unchanged objects identical (`===`) between results, so `memo` skips the 8
  rows whose `user` objects did not change; `ClubCard` still re-renders (its query result changed) and so does
  `BookPanel` (a new `book` object).
- **Data masking (Apollo Client 4):** the query's own result holds only its own fields (`id`, `name`,
  `memberCount`, and references); the book's fields belong to `BookPanel_book`. A change to `averageRating`
  is not a change to `ClubCard`'s data, so only the `useFragment` that selected it re-renders.
- **Relay:** masking is the only mode. Each `useFragment` subscribes to its own fragment's records; the store
  notifies only subscriptions whose data changed.
- First-load counts are not comparable across clients (Relay's `ClubCard` suspends in `useLazyLoadQuery` and
  the suspended render counts too); the experiment compares the counts after `addReview`.

## What it cost

Relay needed a compiler step (`relay-compiler` writes `__generated__/*.graphql.js`), the Babel plugin to replace
the `graphql` tags, fragment names that start with the file's name (`BookPanel_book` in `BookPanel.jsx`), and
ids unique across all types (Shelfie's `b1`, `c1`, `u1` happen to be; Relay's store keys records by `id` alone).
It also met two tool bugs: relay-compiler 20.1.1 panicked ("no entry found for key") on a schema file ending
in `.graphqls`, and Node needed a resolve hook for the extensionless artifact imports. Apollo needed none of
that; data masking (Apollo Client 4) gets the same render counts with an opt-in flag and `useFragment`, without
a compiler. For Shelfie's web app: Apollo Client with data masking, fragments colocated per component; Relay
when the app is large enough that the compiler's guarantees (every component declares what it reads, unused
fields are compile errors) pay for the build step, or when the team wants its pagination and `@defer` support.
