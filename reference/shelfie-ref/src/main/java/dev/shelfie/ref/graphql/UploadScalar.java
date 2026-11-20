package dev.shelfie.ref.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code scalar Upload}: input only. Its value is a {@link MultipartFile} that
 * {@link MultipartGraphQlController} put into the variables; a JSON request cannot produce one.
 */
@Configuration
public class UploadScalar {

    @Bean
    RuntimeWiringConfigurer uploadScalarWiring() {
        GraphQLScalarType upload = GraphQLScalarType.newScalar()
                .name("Upload")
                .description("A file part of a multipart request")
                .coercing(new Coercing<MultipartFile, Void>() {
                    @Override
                    public Void serialize(Object result, GraphQLContext ctx, Locale locale) {
                        throw new CoercingSerializeException("Upload is an input-only scalar");
                    }

                    @Override
                    public MultipartFile parseValue(Object input, GraphQLContext ctx, Locale locale) {
                        if (input instanceof MultipartFile f) return f;
                        throw new CoercingParseValueException(
                                "Upload values come from a multipart request's file parts, not from JSON (got "
                                        + (input == null ? "null" : input.getClass().getSimpleName()) + ")");
                    }

                    @Override
                    public MultipartFile parseLiteral(Value<?> input, CoercedVariables vars, GraphQLContext ctx, Locale locale) {
                        throw new CoercingParseLiteralException("Upload cannot be written as a literal; use a variable");
                    }
                })
                .build();
        return wiring -> wiring.scalar(upload);
    }
}
