# Hints: m11/03

1. The four problems are four Reactor operators, all in `LocalBus`: one more condition in a `filter`, a way
   to thin a stream by time, a hook that runs however a `Flux` ends, and an operator that ends one `Flux`
   when another emits.
2. `doOnComplete` does not run when the client goes away: that is a cancel. `doFinally` runs on all
   three. `takeUntilOther(archived.asFlux().filter(roomId::equals))` ends a stream on archive. A buffer
   that "never loses an event" is exactly what you do not want for typing.
3. `sample(Duration)` keeps one event per period and so loses authors; instead cut the stream into 250 ms
   `window`s and pass each author once per window (`distinct(Typing::author)`), `concatMap`ped back into
   one stream. A sink created with `directBestEffort()` drops events for a subscriber that is not ready.
