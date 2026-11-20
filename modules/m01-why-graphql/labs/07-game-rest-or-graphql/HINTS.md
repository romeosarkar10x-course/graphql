# Hints, REST or GraphQL?

1. First ask: who calls this, how many of them, and do you control their release cycle?
2. Then ask: is the data the same for everyone (cache it), binary or huge (not JSON), pushed to someone not connected (webhook), or shaped differently per screen (GraphQL)?
3. "None of the above" is allowed: a static file or a CDN is the right answer more often than people admit.
