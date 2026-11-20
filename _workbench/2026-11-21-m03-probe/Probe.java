import graphql.*;
import graphql.schema.*;
import graphql.schema.idl.*;
import graphql.scalars.ExtendedScalars;
import java.util.*;

public class Probe {
  record Club(String id, String name, String ownerId) {}
  static GraphQL build(String sdl, RuntimeWiring w) {
    return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), w)).build();
  }
  static void run(String label, GraphQL g, String q, Map<String,Object> vars) {
    var r = g.execute(ExecutionInput.newExecutionInput(q).variables(vars == null ? Map.of() : vars).build());
    System.out.println("## " + label + "\n" + r.toSpecification());
  }
  static void tryBuild(String label, String sdl, RuntimeWiring w) {
    try { build(sdl, w); System.out.println("## " + label + "\nBUILT OK"); }
    catch (Exception e) { System.out.println("## " + label + "\n" + e.getClass().getName() + ": " + e.getMessage()); }
  }
  public static void main(String[] a) {
    String what = a.length > 0 ? a[0] : "all";
    // 1 missing wiring
    String sdl1 = "type Query { club(id: ID!): Club } type Club { id: ID! name: String! owner: User! } type User { id: ID! name: String! }";
    var w1 = RuntimeWiring.newRuntimeWiring().type("Query", t -> t.dataFetcher("club", e -> new Club("c1", "Night Owls", "u1"))).build();
    run("1 missing Club.owner wiring", build(sdl1, w1), "{ club(id: \"c1\") { id name owner { name } } }", null);
    String sdl1b = "type Query { club(id: ID!): Club } type Club { id: ID! name: String! owner: User } type User { id: ID! name: String! }";
    run("1b nullable owner, missing wiring", build(sdl1b, w1), "{ club(id: \"c1\") { id name owner { name } } }", null);
    // 2 Int overflow, ID from long
    String sdl2 = "scalar Long type Query { big: Int small: Int id: ID f: Float bigLong: Long n: Int! }";
    var w2 = RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.GraphQLLong).type("Query", t -> t
      .dataFetcher("big", e -> Long.MAX_VALUE).dataFetcher("small", e -> 42L)
      .dataFetcher("id", e -> 9007199254740993L).dataFetcher("f", e -> 0.1f)
      .dataFetcher("bigLong", e -> Long.MAX_VALUE).dataFetcher("n", e -> 3_000_000_000L)).build();
    run("2 Long.MAX_VALUE as Int etc", build(sdl2, w2), "{ big small id f bigLong }", null);
    run("2b non-null Int overflow", build(sdl2, w2), "{ n }", null);
    String sdl2c = "type Query { echo(n: Int): Int }";
    var w2c = RuntimeWiring.newRuntimeWiring().type("Query", t -> t.dataFetcher("echo", e -> e.getArgument("n"))).build();
    run("2c Int literal 3000000000", build(sdl2c, w2c), "{ echo(n: 3000000000) }", null);
    run("2d Int variable 3000000000", build(sdl2c, w2c), "query($n: Int) { echo(n: $n) }", Map.of("n", 3000000000L));
    run("2e Int variable 1.0", build(sdl2c, w2c), "query($n: Int) { echo(n: $n) }", Map.of("n", 1.0));
    run("2f Int variable \"7\"", build(sdl2c, w2c), "query($n: Int) { echo(n: $n) }", Map.of("n", "7"));
    // 4 extended scalars
    String sdl4 = "scalar DateTime scalar BigDecimal scalar Url type Query { at: DateTime d: BigDecimal u: Url echoAt(at: DateTime!): DateTime echoUrl(u: Url!): Url }";
    var w4 = RuntimeWiring.newRuntimeWiring().scalar(ExtendedScalars.DateTime).scalar(ExtendedScalars.GraphQLBigDecimal).scalar(ExtendedScalars.Url)
      .type("Query", t -> t.dataFetcher("at", e -> java.time.OffsetDateTime.parse("2026-11-21T18:30:00+05:30"))
        .dataFetcher("d", e -> new java.math.BigDecimal("12.50")).dataFetcher("u", e -> "https://shelfie.dev")
        .dataFetcher("echoAt", e -> e.getArgument("at")).dataFetcher("echoUrl", e -> e.getArgument("u"))).build();
    var g4 = build(sdl4, w4);
    run("4 extended scalars out", g4, "{ at d u }", null);
    run("4b DateTime literal bad", g4, "{ echoAt(at: \"2026-13-01T00:00:00Z\") }", null);
    run("4c DateTime variable bad", g4, "query($a: DateTime!) { echoAt(at: $a) }", Map.of("a", "2026-13-01T00:00:00Z"));
    run("4d DateTime no offset", g4, "query($a: DateTime!) { echoAt(at: $a) }", Map.of("a", "2026-11-01T00:00:00"));
    run("4e Url bad", g4, "query($u: Url!) { echoUrl(u: $u) }", Map.of("u", "not a url"));
    // 6 missing type resolver
    String sdl6 = "type Query { node: Node } interface Node { id: ID! } type Book implements Node { id: ID! title: String }";
    tryBuild("6 interface without TypeResolver", sdl6, RuntimeWiring.newRuntimeWiring().build());
    String sdl6u = "type Query { s: S } union S = A | B type A { a: Int } type B { b: Int }";
    tryBuild("6b union without TypeResolver", sdl6u, RuntimeWiring.newRuntimeWiring().build());
    String sdl6n = "type Query { n: Named } interface Node { id: ID! } interface Named implements Node { id: ID! name: String } type U implements Named & Node { id: ID! name: String }";
    tryBuild("6c interface implements interface (with resolver)", sdl6n, RuntimeWiring.newRuntimeWiring().type("Named", t -> t.typeResolver(e -> e.getSchema().getObjectType("U"))).build());
    String sdl6m = "type Query { n: Named } interface Node { id: ID! } interface Named implements Node { id: ID! name: String } type U implements Named { id: ID! name: String }";
    tryBuild("6d object misses transitive interface", sdl6m, RuntimeWiring.newRuntimeWiring().type("Named", t -> t.typeResolver(e -> e.getSchema().getObjectType("U"))).build());
    // 8 oneOf
    String sdl8 = "type Query { book(by: BookLookup!): String } input BookLookup @oneOf { id: ID isbn: String title: String }";
    var w8 = RuntimeWiring.newRuntimeWiring().type("Query", t -> t.dataFetcher("book", e -> String.valueOf((Object) e.getArgument("by")))).build();
    var g8 = build(sdl8, w8);
    run("8a one field", g8, "{ book(by: {isbn: \"9780141439518\"}) }", null);
    run("8b two fields literal", g8, "{ book(by: {id: \"b1\", isbn: \"x\"}) }", null);
    run("8c zero fields", g8, "{ book(by: {}) }", null);
    run("8d null field literal", g8, "{ book(by: {id: null}) }", null);
    run("8e variable two fields", g8, "query($b: BookLookup!) { book(by: $b) }", Map.of("b", Map.of("id", "b1", "title", "t")));
    var m = new HashMap<String,Object>(); m.put("id", null);
    run("8f variable null field", g8, "query($b: BookLookup!) { book(by: $b) }", Map.of("b", m));
    run("8g nullable variable inside", g8, "query($i: ID) { book(by: {id: $i}) }", Map.of());
    run("8h non-null variable inside", g8, "query($i: ID!) { book(by: {id: $i}) }", Map.of("i", "b1"));
    tryBuild("8i input A @oneOf { a: A }", "type Query { x(a: A): Int } input A @oneOf { a: A }", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("8j input A @oneOf { a: A, b: Int }", "type Query { x(a: A): Int } input A @oneOf { a: A b: Int }", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("8k input A { a: A! } (non-oneOf cycle)", "type Query { x(a: A): Int } input A { a: A! }", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("8l oneOf with non-null field", "type Query { x(a: A): Int } input A @oneOf { a: Int! b: Int }", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("8m oneOf with default", "type Query { x(a: A): Int } input A @oneOf { a: Int = 1 b: Int }", RuntimeWiring.newRuntimeWiring().build());
    // 9 deprecation, directives on directives
    tryBuild("9a deprecated arg, input field, enum value", "type Query { x(old: Int @deprecated(reason: \"use y\"), y: Int): Int z(i: I): E } input I { a: Int @deprecated b: Int } enum E { A B @deprecated(reason: \"gone\") }", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("9b deprecated required arg", "type Query { x(old: Int! @deprecated): Int }", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("9c deprecated required input field", "type Query { x(i: I): Int } input I { a: Int! @deprecated }", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("9d directive on directive definition", "directive @meta on DIRECTIVE_DEFINITION directive @tag(name: String!) @meta on FIELD_DEFINITION type Query { x: Int @tag(name: \"a\") }", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("9e extend type across", "type Query { a: Int } extend type Query { b: Int } type Book { id: ID } extend type Book { title: String } extend schema @deprecated", RuntimeWiring.newRuntimeWiring().build());
    tryBuild("9f extend unknown type", "type Query { a: Int } extend type Nope { b: Int }", RuntimeWiring.newRuntimeWiring().build());
  }
}
