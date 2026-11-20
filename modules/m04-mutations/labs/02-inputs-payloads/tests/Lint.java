import graphql.schema.*;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Lab 4.2's mutation lint. Prints "ok <rule>" or "fail <rule>: <where>"; exit 1 on any failure. */
public class Lint {
    static int fails = 0;
    static void check(boolean ok, String rule, String where) {
        if (!ok) { System.out.println("fail " + rule + ": " + where); fails++; }
    }
    public static void main(String[] a) throws Exception {
        GraphQLSchema s = UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse(Files.readString(Path.of(a[0]))));
        GraphQLObjectType m = s.getMutationType();
        if (m == null) { System.out.println("fail has-mutations: no Mutation type"); System.exit(1); }
        List<GraphQLFieldDefinition> fs = m.getFieldDefinitions();
        check(fs.size() >= 5, "at-least-five", fs.size() + " mutations");
        Set<String> verbs = Set.of("create", "update", "delete", "join", "leave", "set", "add", "remove");
        GraphQLObjectType ue = s.getObjectType("UserError");
        check(ue != null && ue.getFieldDefinition("message") != null && ue.getFieldDefinition("field") != null
                && ue.getFieldDefinition("code") != null && GraphQLTypeUtil.unwrapAll(ue.getFieldDefinition("code").getType()) instanceof GraphQLEnumType,
                "user-error-type", "UserError { field message code } with code an enum");
        for (GraphQLFieldDefinition f : fs) {
            String n = f.getName(), cap = Character.toUpperCase(n.charAt(0)) + n.substring(1);
            check(n.matches("[a-z][A-Za-z]*"), "camel-case", n);
            check(verbs.stream().anyMatch(n::startsWith), "verb-first", n + " (start with " + verbs + ")");
            check(f.getArguments().size() == 1 && f.getArguments().get(0).getName().equals("input"), "one-input-argument", n);
            if (f.getArguments().size() == 1) {
                GraphQLInputType t = f.getArguments().get(0).getType();
                check(GraphQLTypeUtil.isNonNull(t) && GraphQLTypeUtil.unwrapNonNull(t) instanceof GraphQLInputObjectType in
                        && in.getName().equals(cap + "Input"), "input-named-" + cap + "Input", n);
                if (GraphQLTypeUtil.unwrapAll(t) instanceof GraphQLInputObjectType in) {
                    for (var field : in.getFieldDefinitions()) {
                        check(!Set.of("userId", "viewerId", "authorId", "clientMutationId").contains(field.getName()), "no-identity-or-relay-fields", in.getName() + "." + field.getName());
                        if (Set.of("status", "action", "visibility", "kind", "type").contains(field.getName()))
                            check(GraphQLTypeUtil.unwrapAll(field.getType()) instanceof GraphQLEnumType, "closed-sets-are-enums", in.getName() + "." + field.getName());
                    }
                }
            }
            GraphQLOutputType r = f.getType();
            boolean payload = GraphQLTypeUtil.isNonNull(r) && GraphQLTypeUtil.unwrapNonNull(r) instanceof GraphQLObjectType p && p.getName().equals(cap + "Payload");
            check(payload, "returns-" + cap + "Payload!", n);
            if (GraphQLTypeUtil.unwrapAll(r) instanceof GraphQLObjectType p) {
                var errs = p.getFieldDefinition("userErrors");
                check(errs != null && GraphQLTypeUtil.simplePrint(errs.getType()).equals("[UserError!]!"), "payload-has-userErrors", p.getName());
                check(p.getFieldDefinitions().size() >= 2, "payload-returns-data", p.getName() + " should return the changed object(s), not only errors");
            }
        }
        System.out.println(fails == 0 ? "ok all rules" : fails + " rule violation(s)");
        System.exit(fails == 0 ? 0 : 1);
    }
}
