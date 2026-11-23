# Toolshed client

A command line on Spring's `HttpSyncGraphQlClient`: typed records for the results, persisted document ids on the
wire (`PersistedDocuments` swaps each operation's text for its id from `persisted-documents.json`).

```bash
java -jar client/target/toolshed-client.jar tools --first 5 --powered
export TOOLSHED_TOKEN=$(java /course/modules/m10-security/contract/Token.java ada --aud toolshed-api)
java -jar client/target/toolshed-client.jar loans
java -jar client/target/toolshed-client.jar borrow t03 --days 3      # exit 3 for a typed failure
java -jar client/target/toolshed-client.jar return l100
java -jar client/target/toolshed-client.jar ui --port 4005           # the web page; COURSE_PUBLISH=4005 to open it on the host
```

`TOOLSHED_URL` changes the router address (default `http://localhost:4000/graphql`). A borrow is retried once on
a network failure with the same idempotency key, so a lost response never borrows twice.
