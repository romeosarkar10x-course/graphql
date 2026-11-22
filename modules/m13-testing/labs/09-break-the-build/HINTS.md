# Hints: m13/09

1. Ask who sends and who receives. For an output (a field's type), making it stricter is safe and looser is
   breaking: clients were promised non-null. For an input (an argument, an input field), it is the other way
   round: looser is safe, stricter is breaking.
2. Required is not the same as non-null: the spec's Required Arguments rule says an argument is required when it
   is non-null **and has no default**. Old queries that leave out an argument with a default still validate.
3. Names are part of the contract even when shapes are not: a renamed type breaks `... on Old` fragments and
   generated types, a renamed argument breaks every query that passes it. A changed default breaks nothing
   loudly, which is exactly why it is dangerous.
