# Hints, jshell speedrun

1. jshell auto-imports `java.util.*` and a few others but not `java.net.http`. Fully qualified names cost characters but save import lines.
2. You do not need a JSON library to pull one string out of a tiny, known response. What does the response look like? Print `r.body()` once.
3. One statement can be one line, however long. Can the `send(...)` call and the print live in a single expression?
