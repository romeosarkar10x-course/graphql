# Mini-project 3 — Records to SDL

**Lectures:** [3.1](../../lectures/3.1-sdl-and-building.md)–[3.7](../../lectures/3.7-unions-sealed.md) · **Grading:** auto + rubric · **Needs:** nothing · **Kind:** mini-project

Schema-first (this module so far) writes SDL and wires Java to it. **Code-first** writes Java and
derives the SDL. DGS, Spring for GraphQL and graphql-java all lean schema-first; libraries such as
graphql-java-annotations, SPQR and Netflix's codegen in reverse go the other way. You write your own in
about 200 lines, with reflection over records and sealed interfaces.

## The rules

| Java | SDL |
|---|---|
| `record` | `type`; components become fields, in declaration order |
| `enum` | `enum`, constants in order |
| `sealed interface` with abstract no-arg methods | `interface` with those fields, sorted by name; records implementing it get `implements` |
| `sealed interface` with no methods | `union` of its permitted subclasses, sorted |
| `String`, `int`/`Integer`, `double`/`Double`/`float`/`Float`, `boolean`/`Boolean` | `String`, `Int`, `Float`, `Boolean` |
| `@Id String` | `ID` |
| `List<T>`, `Set<T>`, any `Collection<T>` | `[T]` |
| `Optional<T>` | `T`, always nullable |
| primitives, and types annotated jspecify `@NonNull` (also list elements: `List<@NonNull User>`) | non-null `!` |
| `@Description("...")` on a type, component, method or enum constant | the SDL description |
| `long`, `Long`, `Map`, anything else | an `IllegalArgumentException` naming `Type.component` (exit 1) |

Types are collected **transitively** from the root classes named on the command line and printed
**sorted by name**, separated by blank lines.

## Do

1. `bin/course start m03/13`, then in `workspace/m03-type-system/13-project-records-to-sdl/`:
   `../../../bin/course shell -c 'mvn -q package && java -jar target/records-to-sdl.jar "dev.shelfie.fixtures.Shelfie\$Query"'`
2. Implement `SdlGenerator` to the rules. The fixtures in `dev/shelfie/fixtures/Shelfie.java` cover
   every row; do not edit them.
3. `DESIGN.md`.

## Done when

`bin/course test m03/13` is green and `bin/course grade m03/13` gives 7/10 or more.

## Group variant

Each person writes the generator alone (90 minutes), then swap fixtures: write one new record or
sealed type that breaks someone else's generator, and fix your own against theirs.
