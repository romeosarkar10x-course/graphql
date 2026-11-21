# Lab 7.5 notes

## 1. Client changes for the fourth kind

| Design | SchemaDiff v1 -> v2 | Client lines for v2 | A v1 client meets a BookFinished item |
|---|---|---|---|
| Interface | SAFE (a new implementation is a new type) | 1 (`... on BookFinished`) | Renders the shared fields: "Asha did something, 2 hours ago". A generic row |
| Union | DANGEROUS (new union member) | 1 | Gets `{ "__typename": "BookFinished" }` and nothing else: a blank row unless the client skips unknown typenames (the harness's mock run: 1 of 4 items blank) |
| Enum discriminator | DANGEROUS (new enum value) | 1 (`book { title }`) | Gets `kind: BOOK_FINISHED`, which its generated enum does not have. Depending on the client, that is an unknown-value fallback or a crash in a `switch` without a default; the shared fields are there |

graphql-inspector 7.0.2 agrees on the union and the enum (⚠ dangerous) and reports the interface case
as a type added (✔ safe) (`_workbench/2026-11-21-m07-probe/inspector-feed.txt`). The line counts are equal: the
difference between the designs is not how much a client edits, it is what happens to the clients
that don't edit.

## 2. What each design is good at

- Interface: the kinds share real behaviour a client can render generically (who, when). Adding kinds is
  the common change, and old clients degrade to the shared fields.
- Union: the members share nothing meaningful (`SearchResult = Book | User | Club`). A client must
  know every member to show anything, which is honest when there is no sensible generic display.
- Enum discriminator: the variants differ in one or two fields, clients mostly filter by kind
  (`feed(kinds: [REVIEW_POSTED])`), or the backing store is one table with a type column. The cost: the
  schema can't say "review is set exactly when kind is REVIEW_POSTED"; only descriptions can.

## 3. Your choice for the Shelfie feed

Interface. Every feed item has an actor and a time, and a feed is where new kinds keep coming: book
finished, poll opened, club renamed. With an interface, a v1 app shows a sensible row for every future
kind with no release. Two client rules make it safe: always select `__typename` and the shared fields
outside any fragment, and treat an unknown `__typename` as "render the generic row", never as an error.
