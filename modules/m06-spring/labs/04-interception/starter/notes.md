# Lab 6.4 notes

1. Trace one request's tenant from the HTTP header to `startsAtLocal`. Name each object it passes
   through.
2. Your interceptor changes the request and the response. What else would you put in each direction in
   a real service? One example each.
3. `@uppercase` could have been implemented in the controller with one `toUpperCase()`. When is wiring
   it in code (a directive) worth the extra machinery?
4. A wrapper that checks `value instanceof String` does nothing to a controller's field. Why, and what
   does that tell you about mixing plain graphql-java code into a Spring app?
