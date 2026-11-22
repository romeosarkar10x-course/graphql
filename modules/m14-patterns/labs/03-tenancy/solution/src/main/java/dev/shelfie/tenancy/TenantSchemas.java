package dev.shelfie.tenancy;

import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.SchemaTransformer;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Per-tenant schema visibility. graphql-java asks the instrumentation for the schema of each execution, before
 * parsing and validation, so a tenant without the premium plan gets a schema with no Club.analytics: introspection
 * does not show it and a query that selects it fails validation, as for any field that does not exist.
 * One transformed schema per plan, built once.
 */
@Component
public class TenantSchemas implements Instrumentation {
    private final Plans plans;
    private final Map<GraphQLSchema, GraphQLSchema> basic = new ConcurrentHashMap<>();

    TenantSchemas(Plans plans) {
        this.plans = plans;
    }

    @Override
    public GraphQLSchema instrumentSchema(GraphQLSchema schema, InstrumentationExecutionParameters parameters, InstrumentationState state) {
        Tenant tenant = parameters.getGraphQLContext().get(Tenant.KEY);
        if (tenant != null && plans.of(tenant).equals("premium")) return schema;
        return basic.computeIfAbsent(schema, TenantSchemas::withoutPremium);
    }

    static GraphQLSchema withoutPremium(GraphQLSchema schema) {
        return SchemaTransformer.transformSchema(schema, new GraphQLTypeVisitorStub() {
            @Override
            public TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition field, TraverserContext<GraphQLSchemaElement> context) {
                if (field.getName().equals("analytics")) return deleteNode(context);
                return TraversalControl.CONTINUE;
            }
        });
    }
}
