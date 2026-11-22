// Checks a composed supergraph (and the API schema clients see) against lab 15.3's brief.
//   java Fed3Check.java supergraph.graphql api.graphql
// prints one line per check: "ok|<check>" or "fail|<check>|<what is missing>"
import graphql.language.*;
import graphql.parser.Parser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Fed3Check {
  static Map<String, TypeDefinition<?>> types = new HashMap<>();
  static Set<String> apiFields = new HashSet<>();

  public static void main(String[] args) throws Exception {
    Document sg = Parser.parse(Files.readString(Path.of(args[0])));
    for (Definition<?> d : sg.getDefinitions()) if (d instanceof TypeDefinition<?> t) types.put(t.getName(), t);
    Document api = Parser.parse(Files.readString(Path.of(args[1])));
    for (Definition<?> d : api.getDefinitions())
      if (d instanceof ObjectTypeDefinition o) o.getFieldDefinitions().forEach(f -> apiFields.add(o.getName() + "." + f.getName()));

    check("entities and keys: User(id) in accounts, Book(id) and Book(isbn) in catalog, Review(id) in reviews, Club(id) in clubs",
        key("User", "ACCOUNTS", "id"), key("Book", "CATALOG", "id"), key("Book", "CATALOG", "isbn"),
        key("Review", "REVIEWS", "id"), key("Club", "CLUBS", "id"));
    check("other teams add fields to entities: User.reviews (reviews), User.clubs and Book.clubsReading (clubs)",
        field("User", "reviews", "REVIEWS", null), field("User", "clubs", "CLUBS", null), field("Book", "clubsReading", "CLUBS", null));
    check("@shareable and @provides: User.handle answered by accounts, and by reviews when reached through Review.author",
        field("User", "handle", "ACCOUNTS", null), field("User", "handle", "REVIEWS", "external: true"),
        field("Review", "author", "REVIEWS", "provides: \"handle\""));
    check("@requires: Book.weeksToRead (clubs) needs catalog's Book.pages",
        field("Book", "weeksToRead", "CLUBS", "requires: \"pages\""), field("Book", "pages", "CATALOG", null));
    check("progressive @override: User.clubCount moving from accounts to clubs, 25% of traffic",
        field("User", "clubCount", "CLUBS", "override: \"accounts\""), field("User", "clubCount", "CLUBS", "overrideLabel: \"percent(25)\""),
        field("User", "clubCount", "ACCOUNTS", null));
    check("@inaccessible: User.email is in the supergraph, not in the API",
        has(types.containsKey("User") && fieldDef("User", "email") != null, "User.email in the supergraph"),
        has(fieldDef("User", "email") != null && fieldDef("User", "email").getDirectives("inaccessible").size() > 0, "@inaccessible on User.email"),
        has(!apiFields.contains("User.email"), "User.email absent from the API schema"));
    check("@interfaceObject: catalog's Media interface (Book, Audiobook) seen by reviews as one type with reviews, reviewCount, averageRating",
        has(types.get("Media") instanceof InterfaceTypeDefinition, "Media is an interface"),
        has(joinType("Media").stream().anyMatch(s -> s.contains("REVIEWS") && s.contains("isInterfaceObject: true")), "reviews declares Media @interfaceObject"),
        implementsMedia("Book"), implementsMedia("Audiobook"),
        field("Media", "reviewCount", "REVIEWS", null), field("Media", "averageRating", "REVIEWS", null));
    check("@tag(name: \"public\"): Query.books, Query.book, Book.title and Book.authors are tagged for the public contract",
        tag("Query", "books"), tag("Query", "book"), tag("Book", "title"), tag("Book", "authors"));
    check("root fields: me, user (accounts), books, book, bookByIsbn (catalog), latestReviews (reviews), clubs, club (clubs)",
        field("Query", "me", "ACCOUNTS", null), field("Query", "user", "ACCOUNTS", null), field("Query", "books", "CATALOG", null),
        field("Query", "book", "CATALOG", null), field("Query", "bookByIsbn", "CATALOG", null),
        field("Query", "latestReviews", "REVIEWS", null), field("Query", "clubs", "CLUBS", null), field("Query", "club", "CLUBS", null));
  }

  static void check(String name, String... missing) {
    List<String> m = Arrays.stream(missing).filter(Objects::nonNull).toList();
    System.out.println(m.isEmpty() ? "ok|" + name : "fail|" + name + "|missing: " + String.join("; ", m));
  }

  static String has(boolean ok, String what) { return ok ? null : what; }

  static List<String> joinType(String type) {
    TypeDefinition<?> t = types.get(type);
    if (t == null) return List.of();
    return t.getDirectives().stream().filter(d -> d.getName().equals("join__type")).map(Fed3Check::print).toList();
  }

  static String key(String type, String graph, String fields) {
    return joinType(type).stream().anyMatch(s -> s.contains("graph: " + graph) && s.contains("key: \"" + fields + "\""))
        ? null : type + " @key(fields: \"" + fields + "\") in " + graph.toLowerCase();
  }

  static FieldDefinition fieldDef(String type, String field) {
    TypeDefinition<?> t = types.get(type);
    List<FieldDefinition> fs = t instanceof ObjectTypeDefinition o ? o.getFieldDefinitions()
        : t instanceof InterfaceTypeDefinition i ? i.getFieldDefinitions() : List.of();
    return fs.stream().filter(f -> f.getName().equals(field)).findFirst().orElse(null);
  }

  /** A field resolved by graph (with a join__field naming it, or a type that only that graph defines). */
  static String field(String type, String field, String graph, String detail) {
    FieldDefinition f = fieldDef(type, field);
    String what = type + "." + field + " from " + graph.toLowerCase() + (detail == null ? "" : " (" + detail + ")");
    if (f == null) return what;
    List<String> joins = f.getDirectives("join__field").stream().map(Fed3Check::print).toList();
    if (joins.stream().anyMatch(s -> s.contains("graph: " + graph) && (detail == null || s.contains(detail)))) return null;
    // no join__field at all: the field belongs to every graph that defines the type
    boolean typeOnlyInGraph = joins.isEmpty() && joinType(type).stream().anyMatch(s -> s.contains("graph: " + graph));
    return typeOnlyInGraph && detail == null ? null : what;
  }

  static String implementsMedia(String type) {
    return types.get(type) instanceof ObjectTypeDefinition o
        && o.getImplements().stream().anyMatch(i -> i instanceof TypeName n && n.getName().equals("Media")) ? null : type + " implements Media";
  }

  static String tag(String type, String field) {
    FieldDefinition f = fieldDef(type, field);
    return f != null && f.getDirectives("tag").stream().map(Fed3Check::print).anyMatch(s -> s.contains("\"public\""))
        ? null : "@tag(name: \"public\") on " + type + "." + field;
  }

  static String print(Directive d) {
    return d.getArguments().stream().map(a -> a.getName() + ": " + AstPrinter.printAst(a.getValue())).collect(Collectors.joining(", "));
  }
}
