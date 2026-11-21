# Hints, mini-project 5

1. Start from what you already have: the wiring from `Shelfie.java` (labs 5.1 and 5.5) and the HTTP rules
   from lab 5.4. Most lines go away when fetchers are one-line lambdas over `Data`, and when `Data`'s
   helpers (`meetupsOf`, `reviewsOf`, `clubsOf`) do the filtering.
2. Arguments arrive with their schema defaults filled in, so `members` always has `first` and `meetups`
   always has `upcomingOnly`; you do not need `getArgumentOrDefault`. One `send(exchange, status, type,
   body, allow)` helper replaces a dozen copies of header code.
3. The status code is a function of three facts: is `data` present, are `errors` present, and which
   media type did you choose. Write it as one expression; re-parse the query only when there is no `data`
   (400 if it does not parse, 422 otherwise).
