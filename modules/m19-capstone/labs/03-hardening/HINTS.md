# Hints, stage 3

1. Do security in this order and check each with `curl` before the next: a bad token is 401, an anonymous
   request is anonymous, a role check, then field rules. Field rules on nullable fields return null and an
   error at that path; on non-null fields they null the parent, so choose which fields they guard.
2. For the N+1 probe you need a number that counts statements, readable over HTTP: a Micrometer counter in a
   DataSource wrapper (datasource-proxy, Module 9) shows at `/actuator/metrics/<name>`. If the large page costs
   more than the small one, the trace of that request shows which field runs once per item.
3. With trusted documents on, your Hurl suite, your client and the probes must all send ids. Build the
   manifest from `operations/` with a script (the id is a hash of the document text), and make the client
   send the id instead of the text (an interceptor in Spring's client can rewrite the request body).
