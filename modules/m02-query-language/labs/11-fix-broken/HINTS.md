# Hints, Fix the broken query

1. The text in brackets is the validation rule, and the path after `@` is where it fired: `(FieldUndefined@[book/name])` means "field `name` under `book` does not exist". The docs pane shows what does.
2. Arguments must exist (`UnknownArgument`), be present when required (`MissingFieldArgument`) and have the right type (`WrongType`); variables must be declared with the name you use (`UndefinedVariable`) and a type that fits where they go (`VariableTypeMismatch`).
3. A fragment on `Book` can only be spread where a `Book` is possible. Fields of a union's members need `... on Member`. Two fields with the same response key must not differ. A syntax error at `<EOF>` means something was opened and never closed.
