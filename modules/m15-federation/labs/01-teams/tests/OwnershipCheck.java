// Checks a team map (ownership.json) against Shelfie's monolith schema, lab 15.1.
//   java OwnershipCheck.java monolith.graphqls ownership.json
// ownership.json: {"types": {"User": "accounts", ...}, "fields": {"Query.me": "accounts", "User.reviews": "reviews", ...},
//                  "keys": {"User": "id", ...}}
// prints "ok|<check>" or "fail|<check>|<detail>" per check
import graphql.language.*;
import graphql.parser.Parser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class OwnershipCheck {
  static final Set<String> TEAMS = Set.of("accounts", "catalog", "reviews", "clubs");

  public static void main(String[] args) throws Exception {
    Document doc = Parser.parse(Files.readString(Path.of(args[0])));
    JsonNode own = JsonMapper.builder().build().readTree(Files.readString(Path.of(args[1])));
    Map<String, ObjectTypeDefinition> objects = new LinkedHashMap<>();
    for (Definition<?> d : doc.getDefinitions()) if (d instanceof ObjectTypeDefinition o) objects.put(o.getName(), o);
    Map<String, String> types = map(own.path("types")), fields = map(own.path("fields")), keys = map(own.path("keys"));

    List<String> bad = new ArrayList<>();
    for (var e : types.entrySet()) if (!TEAMS.contains(e.getValue())) bad.add(e.getKey() + " -> " + e.getValue());
    for (var e : fields.entrySet()) if (!TEAMS.contains(e.getValue())) bad.add(e.getKey() + " -> " + e.getValue());
    report("every owner is one of accounts, catalog, reviews, clubs", bad);

    bad = new ArrayList<>();
    for (String t : objects.keySet()) if (!isRoot(t) && !types.containsKey(t)) bad.add(t);
    report("every type has an owning team", bad);

    bad = new ArrayList<>();
    for (String f : fields.keySet()) {
      String[] p = f.split("\\.", 2);
      if (p.length != 2 || !objects.containsKey(p[0]) || objects.get(p[0]).getFieldDefinitions().stream().noneMatch(x -> x.getName().equals(p[1]))) bad.add(f);
    }
    for (String t : types.keySet()) if (!objects.containsKey(t)) bad.add(t);
    report("ownership.json names only types and fields that exist", bad);

    bad = new ArrayList<>();
    for (String t : List.of("Query", "Mutation"))
      for (FieldDefinition f : objects.get(t).getFieldDefinitions()) if (!fields.containsKey(t + "." + f.getName())) bad.add(t + "." + f.getName());
    report("every Query and Mutation field has an owner", bad);

    Set<String> rootTeams = new TreeSet<>();
    fields.forEach((f, team) -> { if (f.startsWith("Query.")) rootTeams.add(team); });
    report("every team owns at least one Query field", TEAMS.stream().filter(t -> !rootTeams.contains(t)).sorted().toList());

    // Which types are shared across teams: an entity needs a key that every team can use to point at it.
    Set<String> needKey = new TreeSet<>();
    List<String> why = new ArrayList<>();
    for (ObjectTypeDefinition o : objects.values()) for (FieldDefinition f : o.getFieldDefinitions()) {
      String coord = o.getName() + "." + f.getName();
      String fieldTeam = fields.getOrDefault(coord, types.get(o.getName()));
      if (fieldTeam == null) continue;
      if (!isRoot(o.getName()) && !fieldTeam.equals(types.get(o.getName()))) { needKey.add(o.getName()); why.add(o.getName() + " (" + coord + " is " + fieldTeam + "'s)"); }
      String target = named(f.getType());
      if (objects.containsKey(target) && types.containsKey(target) && !fieldTeam.equals(types.get(target))) { needKey.add(target); why.add(target + " (" + coord + " is " + fieldTeam + "'s)"); }
    }
    bad = new ArrayList<>();
    for (String t : needKey) {
      String k = keys.get(t);
      if (k == null) bad.add(t + " has no key");
      else for (String part : k.split("\\s+"))
        if (objects.get(t).getFieldDefinitions().stream().noneMatch(x -> x.getName().equals(part))) bad.add(t + " key field " + part + " does not exist");
    }
    report("every type another team points at or adds to has a key (" + String.join(", ", needKey) + ")", bad);

    List<String> heavy = new ArrayList<>();
    Map<String, Integer> count = new TreeMap<>();
    for (ObjectTypeDefinition o : objects.values()) for (FieldDefinition f : o.getFieldDefinitions()) {
      String team = fields.getOrDefault(o.getName() + "." + f.getName(), types.get(o.getName()));
      if (team != null) count.merge(team, 1, Integer::sum);
    }
    int total = count.values().stream().mapToInt(Integer::intValue).sum();
    count.forEach((team, n) -> { if (n * 2 > total) heavy.add(team + " owns " + n + " of " + total + " fields"); });
    report("no team owns more than half of the graph (" + count + ")", heavy);
  }

  static boolean isRoot(String t) { return t.equals("Query") || t.equals("Mutation"); }

  static String named(Type<?> t) {
    while (!(t instanceof TypeName)) t = t instanceof NonNullType n ? n.getType() : ((ListType) t).getType();
    return ((TypeName) t).getName();
  }

  static Map<String, String> map(JsonNode n) {
    Map<String, String> m = new LinkedHashMap<>();
    n.properties().forEach(e -> m.put(e.getKey(), e.getValue().asString()));
    return m;
  }

  static void report(String name, List<String> problems) {
    System.out.println(problems.isEmpty() ? "ok|" + name : "fail|" + name + "|" + String.join("; ", problems.subList(0, Math.min(8, problems.size()))) + (problems.size() > 8 ? "; ..." : ""));
  }
}
