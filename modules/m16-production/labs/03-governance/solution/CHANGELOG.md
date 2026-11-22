# Shelfie API changelog

## 2026-11-16

### Removed

- `Book.isbn10`. Deprecated since 2026-06-01 ("Use isbn13. ISBN-10 is not issued any more."). Its last use was
  by iOS 2.7.0, which was replaced by 2.8.0 and 2.9.1, 8 days before this release. Use `Book.isbn13`. Evidence:
  `REMOVAL.md`.

### Still deprecated

- `Club.memberCount`: still used by iOS 2.8.0 (22 operations in the last 7 days). It stays until that version's
  traffic is zero for 30 days.
