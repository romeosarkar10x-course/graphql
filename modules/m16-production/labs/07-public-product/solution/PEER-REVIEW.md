# Peer review of the 2027-01 announcement

Solo run: I reviewed my own draft a day later against the checklist, as the reviewer would.

| Question | Draft 1 | Change made |
| --- | --- | --- |
| Can a reader tell in ten seconds whether they are affected? | No: the changes were in the third paragraph | Moved the three changes to a bulleted list at the top |
| Is every date concrete? | "Next autumn" for the retirement | Replaced with 2027-10-01 and a table of dates |
| Does every breaking change have a replacement? | `addReview` said "now needs visibility" without the value to use | Added "`PUBLIC` keeps today's behaviour" |
| Can it be tested before the release? | Not mentioned | Added the sandbox line |
| Does it say what happens after the support date? | No | The table's last row: fall forward to the oldest supported version |

What I would still ask a real reviewer: whether "no operation in the last seven days" is enough evidence to
promise nobody is affected by the `isbn10` removal. It is not: a partner that queries monthly would not show
up in a seven-day window. The announcement now says what the data shows instead of promising.
