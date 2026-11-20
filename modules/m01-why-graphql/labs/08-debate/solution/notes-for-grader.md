# Strong arguments the grader should look for

**For:** partners assemble account views from many resources (round trips, over-fetching); field-level
authorization and consent scopes map onto fields; one typed, introspectable contract with generated
docs; additive evolution without versioning partners; the bank's own mobile apps get one-round-trip screens.

**Against:** open-banking regulation (e.g. PSD2-style standards) often prescribes REST/JSON
specifications, so GraphQL may not even be compliant as the only interface; third parties cannot be
allowlisted, so demand control and cost budgets become mandatory (Module 10); HTTP caching and per-endpoint
audit and rate limiting are harder; partners' tooling and auditors expect OpenAPI; the core system is
batch-oriented and will be hit by arbitrary query shapes.

**Strong verdicts** separate the public partner API (REST, possibly mandated) from the bank's own apps
(GraphQL BFF), or name the deciding regulatory fact.
