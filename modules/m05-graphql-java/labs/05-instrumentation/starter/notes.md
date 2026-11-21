# Lab 5.5 notes

## The slow fields

| Coordinate | max ms | Called how many times in the query? |
|---|---|---|
| | | |

## Questions

1. You measured with `beginFieldFetch`. Try `beginFieldExecution` instead and run `report` again. Which
   coordinates join the list, and why? Which hook would you use to find the field to optimise first?
2. Your state lives in an `InstrumentationState` created per request. What breaks if you keep it in a
   `static` map on a server with concurrent requests?
3. `Book.averageRating` is "only" 120 ms but runs once per club. Estimate the request's total time if
   `Query.clubs` returned 50 clubs, and name two ways to fix it (one now, one from Module 8).
4. Should timings go to `extensions` in production responses? Who could read them, and what would you
   send to your metrics system instead?
