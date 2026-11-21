# Rubric, lab 11.1 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 4 | The diagram: the mutation, where the message is stored and published once, each subscriber's own source stream and filter (why r2's subscriber gets nothing), and that each subscriber's selection set runs separately (the response stream). |
| 3 | The frames from a real run before and after the resolver, over WebSocket, and what SSE sent instead. Invented frames score 0. |
| 3 | The difference between a source stream error (ends the subscription) and a field error in one event (the event carries `errors`), and what a client should do with each. |
