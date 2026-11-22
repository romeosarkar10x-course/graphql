# Hints: m12/01

1. Start with `execute`: build a `Map` with `query` (and `variables` when there are any), turn it into a string
   with `JSON.writeValueAsString`, POST it, and read the body with `JSON.readTree`. `executeNamed` and `field`
   are a few lines on top of it.
2. Decide what each failure is before you parse: no response at all (an `IOException`), a status that is not
   2xx (look at `Content-Type` before trusting the body to be JSON), and a 200 with `errors`. Only the last one
   is a normal `Response`; partial `data` must survive in all three where it exists.
3. For dedupe, keep a `ConcurrentHashMap<String, CompletableFuture<Response>>`. Build the key from the
   document plus the variables serialised with their keys sorted, use `putIfAbsent` so only the first caller
   sends, and remove the entry when the response (or the failure) arrives.
