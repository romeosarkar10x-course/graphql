package dev.shelfie.locales;

import java.util.List;
import java.util.Locale;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Spring hands a controller one Locale: the first of Accept-Language, or the JVM's default when there is no
 * header. Content negotiation needs every range in order ("es, de;q=0.8" means German when there is no Spanish),
 * so the whole list goes into the GraphQLContext as "languages".
 */
@Component
public class LanguageInterceptor implements WebGraphQlInterceptor {
    static final List<Locale.LanguageRange> ENGLISH = Locale.LanguageRange.parse("en");

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        List<Locale.LanguageRange> ranges = parse(request.getHeaders().getFirst("Accept-Language"));
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(c -> c.put("languages", ranges)).build());
        return chain.next(request);
    }

    static List<Locale.LanguageRange> parse(String header) {
        if (header == null || header.isBlank()) return ENGLISH;
        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(header);
            return ranges.isEmpty() ? ENGLISH : ranges;
        } catch (IllegalArgumentException malformed) {
            return ENGLISH;
        }
    }
}
