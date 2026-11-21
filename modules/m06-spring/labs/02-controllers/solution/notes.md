# Lab 6.2 notes

1. `@LocalContextValue` reads from the local context only when it is a `GraphQLContext`, so the parent
   must return a `DataFetcherResult` with `localContext(GraphQLContext.of(Map.of("club", club)))`. Plain
   return values carry no local context. The owner and members fetchers moved to `LabController`, the
   only place that knows a child will need the club.
2. `{id: "c2"}` and `{id: "c2", description: null}` both bind `description` to `null` in a plain record:
   the difference between "the client did not mention it" and "the client wants it empty" is lost
   before the method runs. A patch must keep the first and clear on the second, which is exactly what
   `ArgumentValue.isOmitted()` versus `isPresent()` with a `null` value tells apart. Lab R03 and R04 show
   both; the starter wiped the description in R03 and set the name to null.
3. `@Argument` per parameter is clearest for one or two scalars and documents the method signature.
   `@Arguments` into a record suits search and filter fields with many optional arguments, keeps
   signatures stable as arguments are added, and gives one object to validate (Module 4.5) or pass to a
   repository.
4. I would restrict `@LocalContextValue` to cases like this one, a value only one subtree needs, and
   review each use: it creates an invisible dependency between a parent's fetcher and its children, and
   a child reached another way (`user(id:)`) silently gets null. `@ContextValue` for request-wide data and
   `ArgumentValue` for patches are worth having everywhere.
