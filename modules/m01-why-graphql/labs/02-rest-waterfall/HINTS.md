# Hints, lab 1.2

1. List every piece of the page and which endpoint has it. The club gives you *ids* (`ownerId`, `memberIds`, `currentBookId`); each id is another request. Reviews give you `authorId`s: more requests.
2. `latestReviews` needs the reviewer's *name*, which is not in the review. Members: only the first 5 ids, in order. `nextMeetup`: the first meetup with `"upcoming": true`; the endpoint already sorts soonest first. A club may have no current book.
3. For speed: draw the dependency graph. The club must come first; owner, members, book, reviews and meetups each need only the club; reviewers need the reviews. Start all of the middle layer with `HTTP.sendAsync(...)` before you `join()` any of them. Cache users by id so the owner (also a member) is fetched once.
