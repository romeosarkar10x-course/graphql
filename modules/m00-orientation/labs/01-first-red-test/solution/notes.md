# Lab 0.1 notes (reference)

## 1. The red run
Hurl reports `Assert failure` on the jsonpath line, with `actual: string <Hello from Shelfie!>`
and `expected: string <Hello, World!>`. The HTTP exchange itself worked (status 200 passed); only
the content was wrong.

## 2. Against nothing
`HTTP connection` error: `(7) Failed to connect to localhost port 1 ... Couldn't connect to server`.
No response exists, so no assertion ran. Section 1 is a *contract* failure (the server answered
differently than promised); section 2 is a *transport* failure (no server). Debug them differently:
the first means read the response, the second means check that the server is up and the URL is right.

## 3. One sentence
Hurl tests speak only HTTP and JSON, so the same file grades a Java, Go or TypeScript server and
cannot be satisfied by mocking Java internals.
