package dev.shelfie.mini;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The engine's front door: parse, validate (the six rules), execute. Returns the response as a map. */
public final class MiniGraphQL {
    private MiniGraphQL() {}

    public static Map<String, Object> execute(Schema schema, String query, String operationName, Map<String, Object> variables,
                                              Resolver resolver, Object root) {
        Ast.Document doc;
        try {
            doc = Parser.parse(query);
        } catch (SyntaxError e) {
            return Map.of("errors", List.of(error(e.getMessage(), List.of(new Ast.Loc(e.line, e.column)))));
        }
        List<Validator.Problem> problems = Validator.validate(schema, doc);
        if (!problems.isEmpty()) return Map.of("errors", problems.stream().map(p -> error(p.message(), p.locations())).toList());
        return Executor.execute(schema, doc, operationName, variables, resolver, root);
    }

    private static Map<String, Object> error(String message, List<Ast.Loc> locations) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("message", message);
        if (!locations.isEmpty()) e.put("locations", locations.stream().map(l -> Map.of("line", l.line(), "column", l.column())).toList());
        return e;
    }
}
