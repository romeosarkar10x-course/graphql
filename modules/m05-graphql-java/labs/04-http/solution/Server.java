import com.sun.net.httpserver.HttpServer;
import graphql.GraphQL;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Executors;

/** Lab 5.4: Shelfie over HTTP on the JDK's built-in server. java Server.java [port=4000] */
public class Server {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 4000;
        var http = new GraphQLHttp(GraphQL.newGraphQL(Shelfie.schema(Path.of("shelfie.graphqls"))).build());
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());   // one virtual thread per request
        server.createContext("/graphql", exchange -> {
            try (exchange) {
                var headers = new HashMap<String, String>();
                exchange.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(), String.join(",", v)));
                var r = http.handle(exchange.getRequestMethod(), exchange.getRequestURI().getRawQuery(), headers,
                    exchange.getRequestBody().readAllBytes());
                r.headers().forEach(exchange.getResponseHeaders()::set);
                byte[] out = r.body().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(r.status(), out.length == 0 ? -1 : out.length);
                if (out.length > 0) exchange.getResponseBody().write(out);
            }
        });
        server.start();
        System.out.println("Shelfie on http://localhost:" + port + "/graphql");
    }
}
