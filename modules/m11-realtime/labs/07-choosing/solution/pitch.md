# Two-minute pitch (reference): the unread badge should poll

Shelfie's unread-message badge should be polled every 60 seconds and on app focus, not pushed by a
subscription.

The badge is on screen for every reader who has the app open: 2,000 people at the evening peak, most of
them not in any room. A subscription means 2,000 sockets held open all evening, each with keep-alives, a
place in the load balancer and a reconnect storm after every deploy, to deliver a number that changes a few
times an hour per reader. In our own test, holding 500 sockets and opening them cost more CPU than the
three messages they carried. Polling at 60 seconds is 33 requests a second for all 2,000 readers, each an
indexed count that a cache can absorb, and it needs no bus, no replica fan-out and no gap fill.

The strongest objection is freshness: a reader may see "2 unread" up to a minute after a third message
arrives. But the badge's job is to draw the reader into a room, and once they are in one, the chat
subscription takes over with sub-second delivery. We also poll on app focus, so the number is fresh exactly
when someone looks. If product later wants an instant badge, the subscription can be added for readers
who are already connected for chat, at no extra socket cost: that is the right moment to push, not now.
