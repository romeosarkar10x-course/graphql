# m19/03 Stage 3: client, real time, performance and security

About 7 hours. Make it safe to put in front of people and fast enough not to notice, and give it a client.

## Do

1. **Security** (Module 10): Spring Security with bearer tokens (`bin/course token <user> --aud <your-api>`,
   verified offline with the course public key), roles, field-level authorization for anything private, a
   page limit, a cost or depth limit, and trusted documents (clients send ids from a manifest; anything else is
   refused). In a federated graph the router is where trusted documents and cost limits belong (Module 15).
   Then `send: "documentId"` and `trusted.manifest` in `capstone.json`.
2. **Performance** (Module 9): batch every list resolver (`@BatchMapping` or a DataLoader) and count SQL
   statements (a Micrometer counter around the DataSource, or Hibernate statistics) at a URL the checker can
   read. Run a load test and write `docs/LOAD.md`.
3. **Real time** (Module 11): the subscription or `@defer` from your contract, working end to end.
4. **A Java client** (Module 12): Spring `GraphQlClient` (or DGS codegen) with typed results, as a command line
   (`client.cmd`), plus **one UI of any kind** (`ui`): a web page, a TUI, a desktop window.
5. **Attack yourself**: run Module 10's attacks (and your own) against your stack and write `docs/ATTACKS.md`:
   what you tried, what came back, what stopped it. A script others can re-run is worth more than a table.
6. **The probes:** `auth` (4+ cases: allowed and refused), `expensive`, `n1`, `realtime`.

## Check

```bash
bin/course test m19/03
bin/course grade m19/03
```

## Done when

Green; the attack report has no open finding you have not argued; the load test's numbers are measured, not
estimated.
