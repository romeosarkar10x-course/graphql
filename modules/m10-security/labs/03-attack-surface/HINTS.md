# Hints, lab 10.3

1. Start with introspection: GraphiQL is on (`localhost:4000/graphiql` with the fork started on the host
   port, or the query in `attacks/introspection.graphql`). Ask for deprecated fields too. Look for lists
   that lead back to their parent type (`Club.members` and `Reader.clubs`): that is where fan-out lives.
2. One request can carry many operations' worth of work: aliases let the same field run many times with
   different arguments. The rate limit says "10 requests a minute". Count tokens: graphql-java's parser
   gives up at 15,000.
3. CSRF needs three things from the server: credentials the browser sends by itself, a content type a
   cross-site form can send without a preflight, and no CSRF token check. Sign in with `login`, then send
   the mutation with only `Cookie: shelfie_token=...` and `Content-Type: text/plain`.
