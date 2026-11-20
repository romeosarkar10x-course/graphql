# Hints, mini-project 3

1. Reflection you need: `Class.isRecord()`, `getRecordComponents()`, `isEnum()`, `getEnumConstants()`, `isSealed()`, `getPermittedSubclasses()`, `getInterfaces()`. Collect types with a work queue and a `TreeMap<String, Class<?>>` keyed by simple name: you get transitivity and sorted output in one structure.
2. `@NonNull` from jspecify is a TYPE_USE annotation: it sits on the *type*, not the component. Read it from `rc.getAnnotatedType().isAnnotationPresent(NonNull.class)`. For `List<@NonNull User>`, the annotated type is an `AnnotatedParameterizedType`; `getAnnotatedActualTypeArguments()[0]` is the element, with its own annotations.
3. Write one recursive `typeRef(AnnotatedType)` that returns strings like `[User!]!`: unwrap `Optional` (strip a trailing `!`), wrap collections in `[...]` around a recursive call, and add `!` at the end if the outer type is non-null. Everything else in the generator is printing.
