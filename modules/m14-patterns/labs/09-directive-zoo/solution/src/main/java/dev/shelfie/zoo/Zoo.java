package dev.shelfie.zoo;

import graphql.GraphQLException;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/** The zoo's wirings, built like lab 14.1's: wrap the data fetcher, refuse non-String fields at start-up. */
@Configuration
public class Zoo {
    static final Pattern WORD = Pattern.compile("[A-Za-z]+");
    static final Map<String, String> PIRATE = Map.of("hello", "ahoy", "hi", "ahoy", "my", "me", "friend", "matey",
        "friends", "mateys", "is", "be", "are", "be", "yes", "aye", "you", "ye", "the", "th'");
    static final Map<String, String> EMOJI = Map.of("book", "📚", "books", "📚📚", "love", "❤️", "star", "⭐",
        "stars", "⭐⭐", "sun", "☀️", "night", "🌙", "winter", "❄️", "sea", "🌊");

    @Bean
    RuntimeWiringConfigurer zooWiring(Clock clock) {
        return wiring -> wiring
            .directive("pirate", strings("pirate", (s, env) -> words(s, PIRATE, true) + " Arr!"))
            .directive("emojify", strings("emojify", (s, env) -> words(s, EMOJI, false)))
            .directive("redactOnFridays", strings("redactOnFridays", (s, env) ->
                LocalDate.now(clock).getDayOfWeek() == DayOfWeek.FRIDAY
                    ? (String) env.getAppliedDirective().getArgument("replacement").getValue() : s))
            .directive("mirror", strings("mirror", (s, env) -> new StringBuilder(s).reverse().toString()));
    }

    /** Replaces whole words found in the table, ignoring case; optionally keeps a leading capital. */
    static String words(String s, Map<String, String> table, boolean keepCapital) {
        Matcher m = WORD.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String word = m.group(), to = table.get(word.toLowerCase());
            if (to != null && keepCapital && Character.isUpperCase(word.charAt(0))) to = Character.toUpperCase(to.charAt(0)) + to.substring(1);
            m.appendReplacement(out, Matcher.quoteReplacement(to == null ? word : to));
        }
        return m.appendTail(out).toString();
    }

    static SchemaDirectiveWiring strings(String name, BiFunction<String, SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition>, String> fn) {
        return new SchemaDirectiveWiring() {
            @Override
            public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
                var field = env.getElement();
                var coordinates = FieldCoordinates.coordinates(env.getFieldsContainer(), field);
                if (!(GraphQLTypeUtil.unwrapNonNull(field.getType()) instanceof GraphQLScalarType t && t.getName().equals("String")))
                    throw new GraphQLException("@" + name + " on " + coordinates + ": only allowed on String fields");
                DataFetcher<?> original = env.getCodeRegistry().getDataFetcher(coordinates, field);
                env.getCodeRegistry().dataFetcher(coordinates, (DataFetcher<Object>) e -> {
                    Object v = original.get(e);
                    return v == null ? null : fn.apply(v.toString(), env);
                });
                return field;
            }
        };
    }
}
