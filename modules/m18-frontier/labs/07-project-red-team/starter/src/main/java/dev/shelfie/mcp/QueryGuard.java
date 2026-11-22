package dev.shelfie.mcp;

import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.QueryComplexityCalculator;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.CoercedVariables;
import graphql.language.*;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * What run_graphql lets through: one query operation, valid against Shelfie's schema, no introspection, no deeper
 * than max-depth, no costlier than max-cost (Module 10's function: 1 per field plus children, times `first`,
 * defaults included). The checks run here, before anything is sent, on the document as the agent wrote it.
 */
@Component
class QueryGuard {
    private final Policy policy;
    private final String schemaUrl;
    private volatile GraphQLSchema schema;

    QueryGuard(Policy policy, @Value("${shelfie.graphql-url}") String graphqlUrl) {
        this.policy = policy;
        this.schemaUrl = graphqlUrl + "/schema";
    }

    static final FieldComplexityCalculator COST = (env, childCost) -> {
        Object n = env.getArguments().get("first");
        return n instanceof Integer i ? Math.max(i, 1) * (1 + childCost) : 1 + childCost;
    };

    /** Returns the document to send, or throws PolicyException with the reason. */
    String check(String query) {
        Document doc;
        try {
            doc = Parser.parse(DescriptionStrippingDocumentSource.strip(query));
        } catch (InvalidSyntaxException e) {
            throw new PolicyException("denied: not a GraphQL document (" + e.getMessage() + ")");
        }
        List<OperationDefinition> ops = doc.getDefinitionsOfType(OperationDefinition.class);
        if (ops.size() != 1) throw new PolicyException("denied: send exactly one operation, found " + ops.size());
        OperationDefinition op = ops.getFirst();
        if (op.getOperation() != OperationDefinition.Operation.QUERY)
            throw new PolicyException("denied: run_graphql is read-only; " + op.getOperation().name().toLowerCase()
                    + " operations are not allowed (writes have their own tools and need a human's approval)");
        List<ValidationError> errors = new Validator().validateDocument(schema(), doc, Locale.ENGLISH);
        if (!errors.isEmpty()) throw new PolicyException("denied: invalid against Shelfie: " + errors.getFirst().getDescription());
        Map<String, Object> variables = variableValues(op);
        QueryTraverser traverser = QueryTraverser.newQueryTraverser().schema(schema()).document(doc)
                .operationName(op.getName()).coercedVariables(CoercedVariables.of(variables)).build();
        AtomicInteger depth = new AtomicInteger();
        List<String> introspection = new ArrayList<>();
        traverser.visitPreOrder(new QueryVisitorStub() {
            @Override
            public void visitField(graphql.analysis.QueryVisitorFieldEnvironment env) {
                String name = env.getField().getName();
                if (name.equals("__schema")) introspection.add(name);
                int d = 1;
                for (var p = env.getParentEnvironment(); p != null; p = p.getParentEnvironment()) d++;
                depth.accumulateAndGet(d, Math::max);
            }
        });
        if (!introspection.isEmpty()) throw new PolicyException("denied: introspection (" + introspection.getFirst() + ") is not available to agents");
        if (depth.get() > policy.maxDepth()) throw new PolicyException("denied: depth " + depth.get() + " is over the limit of " + policy.maxDepth());
        int cost = QueryComplexityCalculator.newCalculator().fieldComplexityCalculator(COST).schema(schema()).document(doc)
                .operationName(op.getName()).variables(CoercedVariables.of(variables)).build().calculate();
        if (cost > policy.maxCost()) throw new PolicyException("denied: cost " + cost + " is over the limit of " + policy.maxCost()
                + " (a field costs 1 plus its children, times first or last); ask for fewer items or fewer fields");
        return AstPrinter.printAst(doc);
    }

    /** run_graphql takes no variables, so a variable is its default, or the request is refused. */
    private static Map<String, Object> variableValues(OperationDefinition op) {
        Map<String, Object> values = new HashMap<>();
        for (VariableDefinition v : op.getVariableDefinitions()) {
            switch (v.getDefaultValue()) {
                case IntValue i -> values.put(v.getName(), i.getValue().intValue());
                case BooleanValue b -> values.put(v.getName(), b.isValue());
                case StringValue s -> values.put(v.getName(), s.getValue());
                case EnumValue e -> values.put(v.getName(), e.getName());
                case null -> {
                    if (v.getType() instanceof NonNullType)
                        throw new PolicyException("denied: $" + v.getName() + " has no value; write values into the query");
                }
                default -> throw new PolicyException("denied: $" + v.getName() + ": write values into the query");
            }
        }
        return values;
    }

    private GraphQLSchema schema() {
        if (schema == null) {
            String sdl = RestClient.create().get().uri(schemaUrl).retrieve().body(String.class);
            schema = UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse(sdl));
        }
        return schema;
    }
}
