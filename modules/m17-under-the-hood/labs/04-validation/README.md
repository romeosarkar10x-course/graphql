# Lab 17.4: Validation

Lecture: [17.4 Validation](../../lectures/17.4-validation.md). Grading: auto.

`Validator.validate(Schema, Document)` returns nothing. Implement six rules, each from its section's formal
specification, reporting every violation as a `Problem` named after its rule:

| Rule | Section |
| --- | --- |
| FieldsOnCorrectType | 5.3.1 Field Selections |
| OverlappingFieldsCanBeMerged | 5.3.2 Field Selection Merging (`FieldsInSetCanMerge`, `SameResponseShape`) |
| KnownArgumentNames | 5.4.1 Argument Names (fields and directives) |
| NoFragmentCycles | 5.5.2.2 Fragment Spreads Must Not Form Cycles |
| NoUnusedVariables | 5.8.4 All Variables Used |
| VariablesInAllowedPosition | 5.8.5 All Variable Usages Are Allowed (`IsVariableUsageAllowed`, `AreTypesCompatible`) |

The schema is the reference server's (`reference/shelfie-ref/.../schema.graphqls`); `Schema` gives you types,
fields, possible types and the built-in directives.

The test feeds `contract/validation/invalid/` (40 documents) to your engine and to graphql-java, one rule at a time
(graphql-java 26 can run a single rule), and compares the errors per document and rule. The counts must match for
four rules. For **NoFragmentCycles** and **OverlappingFieldsCanBeMerged** only whether the rule fires is compared:
the spec says a document is invalid, not how many errors to report, and graphql-java counts in its own way (one
error per fragment at either end of a back edge; one per conflicting group and path). Then 20 valid documents
(`contract/validation/valid/` and Module 2's operations) must give no errors at all. Two of the invalid documents
also break a rule outside the six (graphql-java's UnknownDirective and UnusedFragment); only the six are compared.

```bash
bin/course start m17/04
bin/course test m17/04
```

## Experiment

`40-everything.graphql` breaks all six rules at once. Predict your error count per rule before you run it.

## Done when

`bin/course test m17/04` passes.
