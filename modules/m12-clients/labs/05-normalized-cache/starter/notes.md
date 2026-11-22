# Normalised caching: notes

## One write, every query

<!-- Run the live experiment (README) and paste the store it prints. Point at the record that both pages read,
     and explain why the mutation's result reached both without a refetch. -->

## What the cache cannot know without a schema

<!-- Two documents can ask for the same data under different keys, and the cache will not see it. Give two
     examples from Shelfie's schema (think about default argument values and about fields that are the same
     list). What does Apollo Client or Apollo Kotlin use to fix each? -->

## What one write does not update

<!-- Scenario 18: after createReview, which cached data is still stale, and why can no cache work that out
     from the response alone? What are the options (refetch, an update function, a cache redirect, ...)? -->
