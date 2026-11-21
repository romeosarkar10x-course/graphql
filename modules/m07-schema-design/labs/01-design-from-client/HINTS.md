# Hints, lab 7.1

1. Start from one screen's JSON and walk it from the top: every object in the JSON is a type, every key a
   field. Then ask where the data comes from: `role` sits next to a club inside a user's profile, so it
   belongs to neither the user nor the club alone.
2. The shape check compares key paths, so names matter only in the query: if your field is `members` and
   the screen says `memberPreview`, write `memberPreview: members(first: 5)`. The same goes for
   `latestReviews` and `readingClubs`.
3. `missing club.owner.avatarUrl` means the selection under `owner` lacks `avatarUrl`; `extra
   book.reviews[].book.title` means the query selects more than the screen shows. A description lint
   failure names the coordinate (`Club.memberCount`): put a `"..."` string directly above it.
