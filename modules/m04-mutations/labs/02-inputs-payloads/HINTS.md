# Hints, lab 4.2

1. Each original breaks several rules. `addReview`: arguments instead of an input, `userId` from the client, `Boolean` that says nothing about why it failed or what was created. Write down the problems first, then the fix.
2. In `Bind`, `Map.containsKey` is what tells "absent" from "sent as null": `get` returns null for both. Only `Optional` components need the distinction; for the rest, null is null.
3. Generic types: `RecordComponent.getGenericType()` gives `Optional<String>` or `List<String>` as a `ParameterizedType`; `getActualTypeArguments()[0]` is the element type. Recurse on it.
