# Shelfie API changelog

Versions are dated (`YYYY-MM`), released quarterly on the first day of the quarter, and each is supported for at
least 12 months. Request a version in the URL: `https://api.shelfie.example/2027-01/graphql`.

## 2027-01

Released 2027-01-01. Supported until at least 2028-01-01.

### Breaking changes

- `Book.isbn10` is removed (deprecated since 2026-06-01). Use `Book.isbn13`.
- `Club.memberCount` is removed (deprecated since 2026-09-01). Use `Club.memberTotal`, which has the same value.
- `Mutation.addReview` has a new required argument `visibility: ReviewVisibility!`. Pass `PUBLIC` for the old
  behaviour.

### Added

- `Book.subtitle: String`.
- `Club.memberTotal: Int!`, the replacement for `memberCount`.
- `enum ReviewVisibility { PUBLIC CLUBS }`.

Migration guide: [MIGRATION.md](MIGRATION.md).

## 2026-10

Released 2026-10-01. Supported until 2027-10-01. The version in production when this changelog starts.
