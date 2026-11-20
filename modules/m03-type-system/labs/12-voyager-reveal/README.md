# Game 3c — Voyager reveal

**Lecture:** after [3.6 Interfaces](../../lectures/3.6-interfaces.md) · **Grading:** rubric · **Needs:** `bin/course ref up` and a browser with internet access · **Kind:** game

You have queried the reference Shelfie schema for three modules. Draw it from memory, then see it.

## Do

1. **Sketch first, no peeking** (10 minutes): in `sketch.md`, draw the reference server's type graph:
   every type you remember as a box, every field that points at another type as an arrow
   (`Club --owner--> User`). ASCII, Mermaid or a photo of paper (`sketch.png`) are all fine. Mark the
   root types.
2. **Reveal:** open <http://localhost:8080/voyager.html> (GraphQL Voyager 2.1.0 rendering the live
   schema from introspection; the page loads Voyager from jsDelivr). Click around: root types, a union,
   `Mutation`'s inputs.
3. **Score yourself** in `compare.md`: types you drew that exist (+1), types you missed (list them),
   edges you got right (+1), edges you invented (-1). Then answer the questions there.

## Group variant

Everyone sketches alone on paper for 10 minutes. Reveal together on one screen. Score each other's
sketches; the highest score explains their memory trick, the lowest picks the next Voyager feature to
demo (`Skip Relay`, `Sort by alphabet`, root selection).
