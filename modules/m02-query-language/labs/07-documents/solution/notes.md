# Lab 2.7 notes (reference)

## Two operations, no operationName

`Must provide operation name if query contains multiple operations.` (classification ValidationError).

`{ hello } query B { hello }` with operationName `B`: `Validation error (LoneAnonymousOperationViolation)
: Operation 'B' is following anonymous operation`. An anonymous operation is allowed only when it is the
only operation in the document, whatever operationName says.

## Operation descriptions

`Invalid syntax with offending token '"Shows a club card"' at line 1 column 1`. The grammar in the
September 2025 spec allows `Description? OperationType Name? ...` (RFC #1170), but graphql-java 26.1's
parser does not implement it yet. The ops/ folder uses a `# Description:` comment, which every parser
ignores; switch to real descriptions when the implementation catches up.

## Request extensions

`{"data":{"hello":"Hello from Shelfie!"}}`: the server accepted and ignored the map. `"extensions": "x"`
gives HTTP 400: it must be a JSON object. The field is a side channel for things that are not part of
the document: persisted query hashes (`persistedQuery.sha256Hash`, Module 9.6), client names and
versions, tracing flags. Servers and gateways read the keys they know and ignore the rest (RFC #976).

## Why a folder of named operations?

- Names show up in logs, metrics and traces per operation (Module 16), instead of one `/graphql` line.
- Files can be linted, validated in CI against the schema, turned into generated types and registered
  as persisted queries (Modules 9, 13).
