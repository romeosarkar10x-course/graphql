# My exhibit: @mirror

`directive @mirror on FIELD_DEFINITION` reads a String field backwards, letter by letter, the way the mirrors
in Piranesi's House would show it: `Quote.mirrored` is the quote reversed. It is wired like the others, wrapping
the data fetcher and refusing non-String fields at start-up.

Stacking: `@mirror @pirate` mirrors first and the pirate finds no words to replace, so you only get " Arr!" at
the end; `@pirate @mirror` gives "!rrA .thginot mlac eb aes 'ht ...", a pirate in a mirror. Order is the whole
joke, which is the spec's point in "Directive Order Is Significant".

What I learned: `@redactOnFridays` is the one that needs care. "Friday" depends on the zone: at 23:30 UTC on a
Thursday in July it is already Friday in London, so the wiring reads the injected `Clock` in the server's zone,
and the tests pin that clock. A directive that depends on time or on the viewer is no longer a pure function of
the value, and caching layers above it (Module 9) must know that.
