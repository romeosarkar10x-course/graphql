# Lab 0.3 notes

## 1. One field vs twenty
Size in bytes of the SWAPI response when you ask for 1 field of a film, and when you ask for 20.
(`curl ... | wc -c`, or the response size GraphiQL shows.)

| Fields asked | Response bytes |
|---|---|
| 1 | |
| 20 | |

## 2. What decided the size?
Who chose how big the response was: the client or the server? What would a REST `GET /films/1` do instead?

## 3. Docs pane
Name one thing you learned from an API's docs pane that you did not have to read any external documentation for.
