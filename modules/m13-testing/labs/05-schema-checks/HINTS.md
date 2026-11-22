# Hints: m13/05

1. `new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(old, new), reporter)` with a `CapturingReporter`
   gives `DiffEvent`s: `getLevel()`, `getCategory()`, `getTypeName()`, `getFieldName()` and `getComponents()`
   (for an enum value, the value; for a default, the argument). Build the coordinate from those, not from the
   message text.
2. Usage is a walk over each operation against the **old** schema (the one the clients were written for): every
   field selected under its parent type, through inline fragments and fragment spreads; every argument it
   passes; every input and enum type its variables or arguments reach. `QueryTraverser` wants variable values;
   a small recursive walk does not.
3. For validation, `ParseAndValidate.parseAndValidate(newSchema, ExecutionInput...operationName(name))` per
   operation gives errors with locations. For the default-value case, remember which arguments an operation
   passed (`Club.members(first:)`) as well as which fields it selected.
