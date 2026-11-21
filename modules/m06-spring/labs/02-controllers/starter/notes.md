# Lab 6.2 notes

1. Why do `owner` and `members` return `DataFetcherResult` here, and what does Spring require of the local context for `@LocalContextValue` to find `club`?
2. Without `ArgumentValue`, what does `@Argument UpdateClubInput input` give you for `{id: "c2"}` and for `{id: "c2", description: null}`? Why can that not be a patch?
3. `@Argument` per parameter or `@Arguments` into one record: when is each better?
4. Which of these annotations would you ban in a large codebase, if any, and why?
