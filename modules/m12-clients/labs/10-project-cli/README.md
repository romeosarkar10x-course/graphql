# Mini-project 12.10: The Shelfie CLI

Lecture: [12.2 Spring's GraphQL clients](../../lectures/12.2-spring-clients.md) (and 12.1). Grading: auto.
Needs the reference server.

A terminal client for any GraphQL server, built on [JLine](https://jline.org) 4.7, whose Tab completion comes
from the server's own schema (introspection, Module 3):

```
shelfie> { club(id: "c1") { owner { ha<Tab>
shelfie> { club(id: "c1") { owner { handle
   ...> } } }
{
  "club" : { "owner" : { "handle" : "kemia" } }
}
shelfie> :schema Club
  id: ID!
  ...
```

| Mode | Must |
|---|---|
| `<url> -e '<document>'` | print `data` as pretty JSON; errors on stderr, exit 1 (works in the starter) |
| `<url> --complete '<text>'` | print the completions for the end of `<text>`, sorted, one per line: the fields of the type the cursor is in, a field's argument names inside `(`, nothing in an argument's value, the possible types after `... on`, and `query`/`mutation`/`subscription`/`fragment` at the top. No `__` meta fields. One introspection request per run. |
| `<url>` | interactive: Tab completes (a JLine `Completer`, using the lines of the unfinished document too), a document typed over several lines runs when its braces balance, `:schema <Type>` lists fields with arguments and types, `:quit` leaves, history in `~/.shelfie_history` |

The 20 completion cases the test runs are in `tests/completions.tsv` (text, tab, expected).

```bash
bin/course start m12/10
cd workspace/m12-clients/10-project-cli
bin/course shell -c "mvn -q package && java -jar target/shelfie-cli.jar http://shelfie-ref:8080/graphql"
```

Point it at any other GraphQL endpoint you can reach (lab 11's chat server, your Module 6 Shelfie): nothing in
it should know Shelfie.

## Group variant: completion duel

Each player writes five tricky completion cases in the same format (`text<TAB>expected`), checked against their
own CLI and argued for from the spec above: aliases, fragments inside fragments, variables in arguments, object
values, strings with braces in them. Swap files and run everyone's cases against everyone's CLI:

```bash
while IFS=$'\t' read -r t want; do [[ "$(java -jar target/shelfie-cli.jar http://shelfie-ref:8080/graphql \
  --complete "$t" | paste -sd' ')" == "$want" ]] && echo "ok   $t" || echo "MISS $t"; done < friend.tsv
```

A point for each case of yours another CLI misses, a point for each of theirs yours gets.

## Done when

`bin/course test m12/10` passes: `-e`, all 20 completion cases, one request per run, the multi-line
interactive run, `:schema`, and Tab in a real terminal.
