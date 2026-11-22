package dev.shelfie.privacy;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Records every top-level mutation field after execution: who, what (with the arguments, so support can see
 * exactly what was sent) and the outcome. TODO what an audit log may keep, for how long, about whom.
 */
@Component
public class AuditInstrumentation implements Instrumentation {
    private final JdbcClient jdbc;
    private final Users users;

    AuditInstrumentation(JdbcClient jdbc, Users users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult result, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        OperationDefinition op = operation(parameters);
        if (op == null || op.getOperation() != OperationDefinition.Operation.MUTATION) return CompletableFuture.completedFuture(result);
        Viewer viewer = parameters.getGraphQLContext().get(Viewer.KEY);
        String actor = viewer == null ? "anonymous" : actorId(viewer);
        for (var selection : op.getSelectionSet().getSelections()) {
            if (!(selection instanceof Field field)) continue;
            String key = field.getResultKey();
            boolean failed = result.getErrors().stream().map(GraphQLError::getPath)
                .anyMatch(path -> path != null && !path.isEmpty() && key.equals(path.getFirst()));
            List<String> arguments = field.getArguments().stream()
                .map(a -> a.getName() + "=" + graphql.language.AstPrinter.printAst(a.getValue())).toList();
            jdbc.sql("INSERT INTO m14_06.audit_log (actor, operation, field, arguments, outcome) VALUES (?, ?, ?, ?, ?)")
                .params(actor, op.getName(), field.getName(), arguments.toArray(String[]::new), failed ? "error" : "ok").update();
        }
        return CompletableFuture.completedFuture(result);
    }

    /** A viewer whose account no longer exists was just erased (deleteMyAccount audits itself afterwards). */
    String actorId(Viewer viewer) {
        Users.User u = users.byHandle(viewer.handle());
        return u == null ? "erased" : u.id();
    }

    static OperationDefinition operation(InstrumentationExecutionParameters parameters) {
        try {
            Document doc = Parser.parse(parameters.getQuery());
            List<OperationDefinition> ops = doc.getDefinitionsOfType(OperationDefinition.class);
            String name = parameters.getOperation();
            return ops.stream().filter(o -> name == null ? ops.size() == 1 : name.equals(o.getName())).findFirst().orElse(null);
        } catch (RuntimeException unparsable) {
            return null;   // nothing executed
        }
    }
}
