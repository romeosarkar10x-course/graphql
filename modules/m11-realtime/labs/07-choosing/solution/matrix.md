# Choosing the right tool: decision matrix (reference)

Numbers from this module's runs: 500 pollers every 2 s cost 250 requests a second whether or not anything
happened (12.6 s of CPU in 30 s for 3 messages); 500 subscribers cost 1.7 s of CPU to connect, then
about one selection-set execution per subscriber per event, with a median delay of 52 to 84 ms (lab 11.5).
`@defer` moved the club page's first byte from 1.51 s to 0.07 s (lab 11.6).

| # | Feature | Choice | Runner-up | Why (latency, change rate, fan-out, cost, failure) |
|---|---|---|---|---|
| 1 | Meetup chat in a club room | subscription (WebSocket), fed by CDC or a shared bus, with gap fill by query | polling every 2 s | People expect sub-second delivery, and a busy room has many events per minute. Fan-out is per room (tens of people), so each event costs a few executions. On a drop the client reconnects with backoff and catches up with `messages(after:)`, so no message is lost (lab 11.2). Polling would cost 250 requests a second per 500 readers even in silent rooms and add a second of delay on average. |
| 2 | "Ada is typing..." | subscription on the same socket, coalesced per author (250 ms windows) | none: polling is too slow to be useful | Only the latest state matters and it changes constantly; dropping events is correct (lab 11.3). It shares the chat's socket, so it adds no connection. A missed event fixes itself with the next keystroke or expires after a few seconds. |
| 3 | The club page's recommendations panel | `@defer` | a separate query fired after the page renders | It is needed once per page view, not live; the rest of the page is useful without it. `@defer` keeps one request and one document, so the client's cache stays consistent; a second query costs a round trip but works with any server and any client. Needs multipart-capable clients. |
| 4 | Unread-message badge on the home screen | polling (every 30 to 60 s, and on app focus), cached per reader | subscription to a per-reader `unreadCount` | A count that is a minute stale is fine, and it is on screen for readers who are not in any room, so a socket per reader for one number is expensive: 2,000 sockets at peak. Polling at 60 s is 33 requests a second for 2,000 readers, each a cheap indexed count. On a drop nothing special happens. |
| 5 | A partner bookshop told about new reviews of its books | webhook, fed by CDC on the reviews table | the partner polls a `reviews(since:)` query | The receiver is a server, not a user, and may be offline: a webhook with retries, signatures and an idempotency key survives that; a subscription would need the partner to hold a socket open forever. CDC means reviews written by any path (imports, moderation tools) are announced, and the event exists only if the row was committed. |
| 6 | The admin dashboard's "members joined today" counter | polling every 10 s | live query | A handful of admins, a number that changes a few times a minute, seconds of delay are fine. Polling is trivial and stateless; a live query would push a new result on each change but graphql-java has no `@live`, so it would need a custom implementation for 5 users. |

## Cost of each option at Shelfie's scale

20,000 readers, 2,000 online at the evening peak, 400 rooms, a few hundred in rooms at once:

- **Chat by subscription:** a few hundred sockets (only readers inside a room), connection setup once, then
  executions proportional to messages × room size. Scales with activity, not with time.
- **Chat by polling at 2 s:** 2,000 readers × 0.5 a second = 1,000 requests a second at peak, whether rooms
  are busy or not, and a second of extra delay on average.
- **Unread badge by subscription:** 2,000 sockets held all evening to deliver a number that changes a few
  times an hour per reader; by polling at 60 s, 33 requests a second.
- **Webhooks:** one HTTP call per review per partner, plus retries; no standing cost.
- **`@defer`:** no extra requests, the same server work, an earlier first paint.

The pattern: push (subscriptions) when latency matters and events are frequent for the people watching;
poll when staleness is acceptable or the audience is large and mostly idle; webhooks for other servers;
CDC as the source of events whichever way they leave.
