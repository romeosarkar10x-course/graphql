package dev.shelfie.mini;

import dev.shelfie.mini.Ast.Document;
import java.util.List;
import java.util.Map;

/**
 * Lab 17.5: execution (spec 6). GetOperation, CoerceVariableValues, ExecuteCollectedFields, CollectFields,
 * ExecuteField, CoerceArgumentValues, CompleteValue and ResolveAbstractType, with execution errors handled as 6.4.4
 * says. Query fields (and list items) run in parallel on virtual threads; mutation root fields one after another.
 * Field values come from the Resolver; __typename, __schema and __type from Introspection.resolve.
 */
public final class Executor {
    /** A request error (7.1.3): the response has errors and no data. */
    public static final class RequestError extends RuntimeException {
        public RequestError(String message) { super(message); }
    }

    private Executor() {}

    /** ExecuteRequest(): the response as a map with "data" and, when there are any, "errors". */
    public static Map<String, Object> execute(Schema schema, Document doc, String operationName, Map<String, Object> variables,
                                              Resolver resolver, Object root) {
        return Map.of("errors", List.of(Map.of("message", "TODO lab 17.5: the executor")));
    }

    /** An error location as the response writes it: {"line": ..., "column": ...}. */
    static Map<String, Object> location(Ast.Loc l) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("line", l.line());
        m.put("column", l.column());
        return m;
    }
}
