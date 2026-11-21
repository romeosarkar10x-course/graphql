# Hints, game 7a

1. Read the six frozen clients and list every coordinate they touch (`Club.members(first:)`,
   `User.bio`, ...). A change can only make the tower fall if it touches one of those, or changes the
   type of a variable a client declares.
2. Removing or renaming a used field breaks; adding anything holds. For arguments, check how each client
   calls the field: a client that always passes the argument doesn't care whether it gained a `!`.
3. A leaf field that changes from `Int` to `String` still validates in every document that selects it:
   validation knows nothing about how the app parses the value. Bet on the tower, not on the app.
