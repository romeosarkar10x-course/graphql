import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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
            case NULL -> withoutPropagation(graphQL).execute(query);
            case HALT -> halt(graphQL, query);
        };
    }

    /**
     * NULL. graphql-java 26 has the behaviour behind an experimental directive on the operation; no Java
     * server reads an onError request property yet. The directive is added to the parsed and validated
     * document, not to the query text: every other node keeps its source location, so error locations
     * still point into the client's document.
     */
    static GraphQL withoutPropagation(GraphQL graphQL) {
        return graphQL.transform(b -> b.preparsedDocumentProvider((input, parseAndValidate) -> {
            PreparsedDocumentEntry entry = parseAndValidate.apply(input);
            if (entry.hasErrors()) return CompletableFuture.completedFuture(entry);
            return CompletableFuture.completedFuture(new PreparsedDocumentEntry(disablePropagation(entry.getDocument())));
        }));
    }

    static Document disablePropagation(Document doc) {
        var definitions = doc.getDefinitions().stream().map(d -> d instanceof OperationDefinition op
            ? op.transform(b -> {
                var directives = new ArrayList<>(op.getDirectives());
                directives.add(Directive.newDirective().name("experimental_disableErrorPropagation").build());
                b.directives(directives);
            })
            : d).toList();
        return doc.transform(b -> b.definitions(definitions));
    }

    /**
     * HALT. The first field error cancels the execution, so no further fetcher starts, and the answer is
     * data: null with that one error. GraphQL.transform keeps the strategies it was built with, so the
     * exception handler has to arrive inside a new strategy.
     */
    static ExecutionResult halt(GraphQL graphQL, String query) {
        var input = ExecutionInput.newExecutionInput(query).build();
        var first = new AtomicReference<GraphQLError>();
        var defaults = new SimpleDataFetcherExceptionHandler();
        var strategy = new AsyncExecutionStrategy(params -> defaults.handleException(params).thenApply(result -> {
            if (first.compareAndSet(null, result.getErrors().getFirst())) input.cancel();
            return result;
        }));
        ExecutionResult result = graphQL.transform(b -> b.queryExecutionStrategy(strategy)).execute(input);
        GraphQLError error = first.get() != null ? first.get() : result.getErrors().isEmpty() ? null : result.getErrors().getFirst();
        return error == null ? result : ExecutionResultImpl.newExecutionResult().data(null).errors(List.of(error)).build();
    }

    public static void main(String[] args) {
        var json = JsonMapper.builder().build();
        for (Mode mode : Mode.values()) {
            if (args.length > 0 && !mode.name().equals(args[0])) continue;
            Page.FETCHES.set(0);
            var result = execute(Page.graphQL(), Page.QUERY, mode);
            System.out.println(mode + " (" + Page.FETCHES.get() + " fetches)");
            System.out.println("    " + json.writeValueAsString(result.toSpecification()));
        }
    }
}
