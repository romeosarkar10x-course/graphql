import graphql.ExecutionResult;
import graphql.GraphQL;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 8.8: a prototype of RFC #1163's request property onError: PROPAGATE (today's behaviour), NULL (no
 * propagation: the null stays where the error happened) and HALT (stop at the first error, data: null).
 *   bin/course java OnError.java [PROPAGATE|NULL|HALT]
 */
public class OnError {
    public enum Mode { PROPAGATE, NULL, HALT }

    public static ExecutionResult execute(GraphQL graphQL, String query, Mode mode) {
        return switch (mode) {
            case PROPAGATE -> graphQL.execute(query);
            case NULL -> throw new UnsupportedOperationException("TODO: lab 8.8 step 1");
            case HALT -> throw new UnsupportedOperationException("TODO: lab 8.8 step 2");
        };
    }

    public static void main(String[] args) {
        var json = JsonMapper.builder().build();
        for (Mode mode : Mode.values()) {
            if (args.length > 0 && !mode.name().equals(args[0])) continue;
            Page.FETCHES.set(0);
            try {
                var result = execute(Page.graphQL(), Page.QUERY, mode);
                System.out.println(mode + " (" + Page.FETCHES.get() + " fetches)");
                System.out.println("    " + json.writeValueAsString(result.toSpecification()));
            } catch (UnsupportedOperationException e) {
                System.out.println(mode + ": " + e.getMessage());
            }
        }
    }
}
