package dev.toolshed.common;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** The contract's DateTime: RFC 3339 in UTC, to the second (2026-11-23T10:15:00Z). */
public final class DateTimeScalar {
    private DateTimeScalar() {}

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("RFC 3339, with an offset")
            .coercing(new Coercing<OffsetDateTime, String>() {
                @Override
                public String serialize(Object value, GraphQLContext context, Locale locale) {
                    if (value instanceof OffsetDateTime t) return format(t);
                    throw new CoercingSerializeException("DateTime expects an OffsetDateTime, got " + value.getClass().getSimpleName());
                }

                @Override
                public OffsetDateTime parseValue(Object input, GraphQLContext context, Locale locale) {
                    try {
                        return OffsetDateTime.parse(input.toString());
                    } catch (DateTimeParseException e) {
                        throw new CoercingParseValueException("not an RFC 3339 date-time: " + input);
                    }
                }

                @Override
                public OffsetDateTime parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext context, Locale locale) {
                    if (input instanceof StringValue s) return parseValue(s.getValue(), context, locale);
                    throw new CoercingParseLiteralException("DateTime is a string");
                }
            })
            .build();

    public static String format(OffsetDateTime t) {
        return t.withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
