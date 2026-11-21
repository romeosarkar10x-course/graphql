# Hints, game 10.9

1. Map the surface before you attack it: full introspection with deprecated fields, every argument, every
   input type's fields (descriptions say more than they should). Then list what an anonymous caller,
   ben, and an admin can each reach, and look for two paths to the same data with different checks.
2. Six flags need nothing but a well-chosen query or argument (introspection, enumeration, a second path
   to a private thing, an error, injection, fan-out). The others need you to be someone you are not, or to
   do more than the server meant to allow per request: tokens, input fields, aliases, a socket, a hash.
3. For the token, decode ben's (`cut -d. -f2 | base64 -d`) and ask what the server would do with a token that
   has no signature at all. For the "persisted only" mutation, ask what makes a query "persisted" to this
   server, and whether you can make one yourself.
