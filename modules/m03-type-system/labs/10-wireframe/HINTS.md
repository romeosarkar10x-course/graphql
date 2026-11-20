# Hints, game 3a

1. Start from the screens, not the database: list every visible value per screen, then ask which type owns it. "8 members" is a field on Club, not a list you count on the client.
2. Things that depend on who is looking (`[ Join ]`, "My shelf") need a notion of the viewer: a `viewer` root field, or fields like `Club.viewerIsMember`. Search mixes three types with nothing in common but a name: union or interface?
3. Check the nullability of each field against one question: "what does the screen show when this is missing?" A missing banner shows a placeholder (nullable); a club without a name is not a club (non-null). Lists that grow ("more reviews") need an argument now, even before Module 8's pagination.
