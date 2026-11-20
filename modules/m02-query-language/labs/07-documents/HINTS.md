# Hints, lab 2.7

1. Most of these exist already: labs 1.3 (ClubPage), 2.1 (ClubCard), 2.2 (BookSearch), 2.3 (CompareClubs), 2.4 (the fragments) and 2.5 (the variant card). Copy, rename, trim.
2. The test runs each file with the variables in its `.json` file and `operationName` set to the file's operation. A root field that returns null (an id that does not exist) fails the test: use ids from the seed data (c1 to c3, u1 to u12, b1 to b10).
3. A named fragment must be defined in the same file that spreads it, because each file is sent on its own. `__typename` is a good first field in a union selection.
