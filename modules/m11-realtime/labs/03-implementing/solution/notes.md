# Implementing subscriptions: notes (reference)

## The flood, before and after

10,000 typing events a second for 3 s from 3 bots in room r2; a fast client (Ada) and a slow one (Ben,
20 frames a second). `_workbench/2026-11-21-m11-probe/lab03-flood.txt`, 2026-11-21, `-Xmx256m`:

| | fast client | slow client | slow client's last event |
|---|---|---|---|
| starter (buffered sink, no thinning) | 30,000 events in 3 s | 236 by t = 12 s, and 29,764 still queued | still arriving at 12 s; at 20 a second the backlog lasts about 25 minutes |
| fix (250 ms windows, one event per author) | 39 | 39 | 3.1 s, right after the flood |

With the starter, Ben's screen would show "bot2 is typing" for 25 minutes after the bots stopped. The
server's memory stayed flat (RSS 423 MB throughout), so the backlog most likely sat in the sink's buffer
and the socket buffers on the way to the slow reader (not measured separately). Every event was
delivered, and that was the bug.

## Why each operator

- `filter(t -> ... && !t.author().equals(me))`: per subscription, so every subscriber gets its own view
  of one shared stream.
- `window(Duration.ofMillis(250)).concatMap(w -> w.distinct(Typing::author))`: at most one event per
  author per 250 ms. `sample(250ms)` would keep one event per window, so with three people typing two
  would vanish.
- `directBestEffort()` sink plus `onBackpressureLatest()`: a subscriber that is not ready misses events
  instead of making the sink buffer for it or stall the others. The starter's `onBackpressureBuffer`
  "never loses one", which is the wrong promise for typing.
- `doFinally`: a client that disconnects cancels its subscription. `doOnComplete` never runs then, so the
  starter's `watching` count only grew. `doFinally` runs on complete, error and cancel.
- `takeUntilOther(archived.filter(roomId::equals))`: archiving ends the source stream, Spring sends
  `complete`, and `doFinally` releases the count.

## When dropping events is right

Dropping or coalescing is right for state where only the latest value matters: typing, presence, cursor
positions, a live score. It is wrong for events that each mean something: chat messages, bids,
payments. For those, a slow client should get a bounded buffer and then be disconnected with an error
(or a `complete`), and catch up with a query, as lab 11.2's client does with `messages(after:)`. Buffering
without a bound only moves the problem into memory or socket buffers, and the client sees it as lag.
