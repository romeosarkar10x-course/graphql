# Hints, lab 1.3

1. In GraphiQL, put the cursor inside `club(id: $id) { }` and press Ctrl+Space: those are your fields. Every field that returns an object (`owner`, `members`, `currentBook`) needs its own `{ ... }`.
2. The reviewer's name is a field of the review's `author`. Lists are cut with arguments: `members(first: 5)`, `reviews(last: 3)`. The reviews hang off `currentBook`, not off `club`.
3. `meetups(upcomingOnly: true) { title startsAt }` comes back soonest first: `nextMeetup` is element 0, or null when the list is empty. `currentBook` can be null: check `isNull()` before reading it.
