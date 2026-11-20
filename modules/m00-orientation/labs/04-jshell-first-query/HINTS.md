# Hints, lab 0.4

1. A GraphQL request is just JSON: a map with a `query` string, an optional `variables` map and an optional `operationName`. `mapper.writeValueAsString(Map.of(...))` turns a Java map into that JSON.
2. Variables are declared in the operation's signature, `query Name($var: Type!) { ... }`, and used as `$var` inside it. The value travels separately in `variables`. For reading the answer, `JsonNode.at("/a/b/c")` walks a path in one call.
3. `HttpRequest.newBuilder(URI.create(base + "/graphql")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build()`, then `HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())`. For the status codes, `curl -s -o /dev/null -w '%{http_code}'` prints only the code.
