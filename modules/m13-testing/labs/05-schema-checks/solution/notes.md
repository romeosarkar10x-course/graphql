# Lab 13.5 notes

## 1. The pull request that breaks a client

Scenario 05 makes `books(titleContains:)` required. graphql-inspector says "breaking" and stops there.
SchemaCheck says which operation breaks and how: Android's `Search` sends `$text: String`, which no longer
validates (`VariableTypeMismatch` at Search.graphql:2:24), so every search from that app fails before a
resolver runs. That line is what a reviewer needs in the pull request; "1 breaking change" is not.

## 2. Breaking but unused

Scenario 03 removes `Club.createdAt`. It is breaking by the rules, but none of the five known operations
selects it, so SchemaCheck passes and says so. That is what a registry check (Hive, GraphOS) does with
real traffic: it fails only for operations seen in the last N days. The risk is the clients you do not
know: a partner's script, an old app version. In-repo clients are a floor, not the whole truth; a registry
with usage reporting is the better source when the API is public.

## 3. Where the tools disagree

On the same scenarios, graphql-inspector 7.0.2 calls a deprecation safe and graphql-java's SchemaDiff
calls it dangerous; both call a new enum value and a changed default dangerous. Inspector's message for
the default is better ("changed from 10 to 5"); graphql-java's is cut off ("of type 'Club"). Precision
matters for defaults: ClubPage passes `first: 8`, so changing the default from 10 to 5 cannot reach it,
and SchemaCheck only lists `Members`, which leaves the argument out.

## 4. Why both checks in the workflow

Inspector annotates the diff line by line in the pull request and knows nothing about clients, so it is set
not to fail. SchemaCheck fails the build only when a known client is hurt and writes the reason into the
job summary. One explains the change, the other decides.
