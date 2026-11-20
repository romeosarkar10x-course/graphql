# Lab 1.2 notes

## 1. Cost of the page, REST
| Club | Requests | Bytes | ms, no latency | ms, 700 ms latency |
|---|---|---|---|---|
| c1 | | | | |

(`bin/course toxi latency 700`, then run with `SHELFIE_URL=http://toxiproxy:8666`; `bin/course toxi reset` after.)

## 2. The dependency graph
Draw (ASCII is fine) which requests depend on which. How many *sequential* round trips does the page
need at minimum, however cleverly you parallelise? Does your program reach that minimum?

## 3. Bytes you threw away
Roughly what share of the bytes you downloaded ended up on the page?
