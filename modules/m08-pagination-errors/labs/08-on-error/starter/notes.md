# Lab 8.8 notes

## Q1. The three modes on Page.QUERY: what does each return, how many fetchers ran, and how many fields did the cache keep?

| Mode | `data` | errors | fetchers | fields cached |
|---|---|---|---|---|
| PROPAGATE | | | | |
| NULL | | | | |
| HALT | | | | |

## Q2. With NULL, `rating: Float!` came back as `null`. Is the schema lying? What must a client do so it never reads that null as a value?

## Q3. Which kind of client wants HALT, and what does the server gain from it?

## Q4. Your prototype is built on an experimental directive. What would have to be true before you turned NULL on for a production API?
