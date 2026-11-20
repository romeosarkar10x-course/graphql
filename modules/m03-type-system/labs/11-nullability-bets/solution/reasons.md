# Reasons (reference)

1. Book.title: non-null. Same row as the id, required at creation; a book without a title is a bug, and failing loudly is right.
2. Book.coverUrl: nullable. A third party can be down or have no image; the page should show a placeholder, not vanish.
3. Club.owner: nullable. The owner can delete their account and the club should survive; it is low stake because "transfer ownership on deletion" is a valid alternative design.
4. Review.author: nullable. Deleted accounts keep their reviews ("former member").
5. Book.averageRating: nullable. No reviews means no mean; 0 would be a lie.
6. User.handle: non-null. Required, same row, unique.
7. Club.memberCount: non-null. Computed in-process from the same row; an empty club has 0, not null.
8. Club.currentBook: nullable. Clubs are between books for part of every month.
