# Hints, mini-project 10

1. `com.sun.net.httpserver.HttpServer` with one context per path, and `java.net.http.HttpClient` to the
   upstream. Copy the status, the `Content-Type` and the bytes back exactly; parse the JSON only to read
   the cost.
2. The cost is in `extensions.cost.requestedQueryCost` on success and in `errors[0].extensions.cost` when
   throttled (`errors[0].extensions.code == "THROTTLED"`). Throttled spends nothing.
3. Keep `key -> minute -> counters`, with the minute formatted in UTC (`yyyy-MM-dd'T'HH:mm`), in concurrent
   maps: the server handles requests in parallel. The page can be a string constant with a small script
   that fetches `/meter`.
