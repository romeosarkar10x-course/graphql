# Hints: m15/01

1. Start from data, not from types: a field belongs to the team whose database it is computed from.
   `Book.averageRating` sits on a book and is computed from reviews.
2. After the first pass, list every field whose type is owned by another team (`Review.author: User`,
   `Club.currentBook: Book`), and every field you gave to a team other than the type's owner. Those two lists
   are exactly the types that need a key.
3. If one team owns more than half the graph, look for a type that came along with something else (meetups came
   with clubs). For the estimate, walk one PR at a time: who opens it, who must approve it, what must be deployed
   before it is useful.
