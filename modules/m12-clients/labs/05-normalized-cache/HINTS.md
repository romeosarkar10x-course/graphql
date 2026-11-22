# Hints: m12/05

1. Write is a walk over the document and the data together: for each field of the selection set, take the
   data under its *response key* (alias or name), store it under its *storage key* (name plus sorted
   arguments), and recurse into its selection set. Read is the same walk over the document and the store.
2. When you meet an object with `__typename` and `id`, store its fields in that record (merge, never replace)
   and put `{"__ref": "Typename:id"}` where it was. Arguments come from the AST: `IntValue`, `StringValue`,
   `VariableReference` (look it up in the variables, and leave the argument out when the variable is absent),
   `ObjectValue`, ... into a `TreeMap`, then JSON for the key.
3. Make "missing" different from `null`: `record.containsKey(key)` false is a miss, a stored `null` is data.
   A sentinel object (`static final Object MISS = new Object()`) that every level returns upward works well,
   and a mutation's root goes into a throwaway map so only its entities stay.
