# Choosing the right tool: decision matrix

Options: **query** (plain request, maybe cached), **polling**, **subscription** (WebSocket or SSE),
**`@defer`**, **live query**, **webhook**, **CDC** feeding one of the others. For each feature: the choice,
the runner-up, and why, in terms of latency needed, how often it changes, how many clients, cost per
client, and what happens when a connection drops.

| # | Feature | Choice | Runner-up | Why (latency, change rate, fan-out, cost, failure) |
|---|---|---|---|---|
| 1 | Meetup chat in a club room | | | |
| 2 | "Ada is typing..." | | | |
| 3 | The club page's recommendations panel | | | |
| 4 | Unread-message badge on the home screen | | | |
| 5 | A partner bookshop told about new reviews of its books | | | |
| 6 | The admin dashboard's "members joined today" counter | | | |

## Cost of each option at Shelfie's scale

<!-- 20,000 readers, 2,000 online at the evening peak, 400 rooms. Use your lab 11.5 numbers. -->
