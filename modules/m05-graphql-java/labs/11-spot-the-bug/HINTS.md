# Hints, game 5b

1. Four of the bugs only show up when more than one thing happens at once. For each fetcher ask: which
   thread runs this, and what else touches the same object at the same time?
2. For the other four, ask what the fetcher assumes: about its source's type, about arguments a client
   controls (including an explicit `null`), about which id it is holding, and about whether the list the
   store returned is safe to change.
3. B8 is the one that hangs. Count the threads in `POOL`, count the tasks one `clubs` query submits to it,
   and look at what each task waits for.
