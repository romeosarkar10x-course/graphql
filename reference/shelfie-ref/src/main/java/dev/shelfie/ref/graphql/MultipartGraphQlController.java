package dev.shelfie.ref.graphql;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * The GraphQL multipart request convention (github.com/jaydenseric/graphql-multipart-request-spec),
 * Module 4.6. Spring for GraphQL does not implement it, so this controller does, in about 60 lines:
 *
 * <ol>
 *   <li>{@code operations}: the usual JSON body, with {@code null} where each file goes;</li>
 *   <li>{@code map}: which file part replaces which {@code null}, e.g. {@code {"0": ["variables.file"]}};</li>
 *   <li>the file parts, named by the map's keys.</li>
 * </ol>
 *
 * A {@code multipart/form-data} POST is a CORS "simple request": a browser sends it cross-site with
 * the victim's cookies and no preflight. So the server refuses it unless a header no HTML form can
 * set is present ({@code GraphQL-Require-Preflight}), which forces a preflight.
 *
 * <p>A router function, not a {@code @PostMapping}: Spring for GraphQL's own router function owns
 * {@code POST /graphql} and answers 415 to multipart before any annotated controller is asked.
 */
@Configuration
public class MultipartGraphQlController {

    private final WebGraphQlHandler handler;
    private final ObjectMapper json;
    private final StandardServletMultipartResolver multipart = new StandardServletMultipartResolver();

    public MultipartGraphQlController(WebGraphQlHandler handler, ObjectMapper json) {
        this.handler = handler;
        this.json = json;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    RouterFunction<ServerResponse> multipartGraphQl() {
        return RouterFunctions.route(RequestPredicates.POST("/graphql")
                .and(RequestPredicates.contentType(MediaType.MULTIPART_FORM_DATA)), this::handle);
    }

    private static ServerResponse error(String message) {
        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("errors", List.of(Map.of("message", message))));
    }

    @SuppressWarnings("unchecked")
    ServerResponse handle(ServerRequest req) {
        if (req.headers().firstHeader("GraphQL-Require-Preflight") == null) {
            return error("Multipart requests need a GraphQL-Require-Preflight header (CSRF protection, Module 4.6)");
        }
        HttpServletRequest servlet = req.servletRequest();
        MultipartHttpServletRequest request = servlet instanceof MultipartHttpServletRequest m ? m : multipart.resolveMultipart(servlet);
        String operations = request.getParameter("operations");
        String map = request.getParameter("map");
        if (operations == null || map == null) return error("Multipart requests need 'operations' and 'map' fields, in that order");
        Map<String, Object> body = json.readValue(operations, LinkedHashMap.class);
        Map<String, List<String>> fileMap = json.readValue(map, LinkedHashMap.class);
        for (var entry : fileMap.entrySet()) {
            MultipartFile file = request.getFile(entry.getKey());
            if (file == null) return error("map names file part '" + entry.getKey() + "' but the request has no such part");
            for (String path : entry.getValue()) {
                if (!place(body, path, file)) return error("map path '" + path + "' does not point at a null in operations");
            }
        }
        HttpHeaders headers = new HttpHeaders();
        servlet.getHeaderNames().asIterator().forEachRemaining(h -> headers.add(h, servlet.getHeader(h)));
        WebGraphQlRequest gql = new WebGraphQlRequest(URI.create(servlet.getRequestURL().toString()), headers,
                new LinkedMultiValueMap<>(), null, Map.of(), body, UUID.randomUUID().toString(), Locale.getDefault());
        var response = handler.handleRequest(gql).block();
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(response.toMap());
    }

    /** Puts {@code file} at a dotted path such as {@code variables.file} or {@code variables.files.0}. */
    @SuppressWarnings("unchecked")
    private static boolean place(Map<String, Object> root, String path, MultipartFile file) {
        String[] parts = path.split("\\.");
        Object node = root;
        for (int i = 0; i < parts.length - 1; i++) {
            node = step(node, parts[i]);
            if (node == null) return false;
        }
        String last = parts[parts.length - 1];
        if (node instanceof Map<?, ?> m && m.containsKey(last) && m.get(last) == null) {
            ((Map<String, Object>) m).put(last, file);
            return true;
        }
        if (node instanceof List<?> l && last.matches("\\d+") && Integer.parseInt(last) < l.size() && l.get(Integer.parseInt(last)) == null) {
            ((List<Object>) l).set(Integer.parseInt(last), file);
            return true;
        }
        return false;
    }

    private static Object step(Object node, String key) {
        if (node instanceof Map<?, ?> m) return m.get(key);
        if (node instanceof List<?> l && key.matches("\\d+") && Integer.parseInt(key) < l.size()) return l.get(Integer.parseInt(key));
        return null;
    }

}
