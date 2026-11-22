# Hints: m12/11

1. Fix the feature in one sentence before writing code: the exact query, the exact mutation and its
   selection. Every difference you find later should come from the client, not from three different features.
2. For request counts, reset with `POST /_course/reset` and read `GET /_course/stats` around each run, or put
   `contract/Capture.java` between the client and Shelfie. The Spring client has no cache: decide whether it
   refetches or merges the mutation's payload, and say which you chose.
3. Write the "what broke" row as you go, with the error text; it is the most useful row a year from now, and
   the hardest to remember afterwards.
