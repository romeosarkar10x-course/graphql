# Hints, game 3b

1. Ask two questions per field: can it be legitimately absent in normal operation? Can producing it fail independently of its parent?
2. Non-null is a promise for the life of the API: making a nullable field non-null later breaks no client, but making a non-null field nullable does (clients stop checking for null). When unsure, which direction is the cheap mistake?
3. Data owned by someone else (another service, another user's account) is the classic nullable case; data in the same row and required at creation is the classic non-null case. Computed values that have a natural zero are not "missing" when the input is empty.
