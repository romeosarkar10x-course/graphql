# Lab 4.4 notes

## Before and after

| Run | Starter: reviews after 50 identical requests | Yours |
|---|---|---|
| 1 | | |
| 2 | | |
| 3 | | |

Why does the starter's count vary between runs?

## Why the database, not Java

Could a `synchronized` block or a `ConcurrentHashMap` of keys replace the primary key? When does that
stop working?

## Optimistic vs pessimistic

When would you lock the club row instead (`SELECT ... FOR UPDATE`)? One case each way.

## The job pattern

What does a client do if its `importBooks` response is lost? What is missing from this lab's version
for that case?
