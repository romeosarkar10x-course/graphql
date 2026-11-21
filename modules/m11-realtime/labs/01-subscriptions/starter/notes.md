# Subscriptions: notes

## One chat message, from mutation to every subscriber

<!-- Diagram it: the sendMessage request, where the message is stored and published, how each subscriber's
     source stream gets it (and why r2's subscriber does not), and where its selection set is executed. -->

## What the client received when the source stream broke

<!-- Paste the frames from `bin/course java modules/m11-realtime/contract/Ws.java ws://localhost:4000/graphql
     'subscription { countdown(from: 5, failAt: 2) }'` before and after your resolver. Then try SSE with curl. -->

## Why it matters
