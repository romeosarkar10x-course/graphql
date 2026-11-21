import graphql.GraphQL;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/** Bonus, lab 5.4: the same GraphQLHttp behind Javalin 7. Run from javalin/: java -cp ... JavalinServer [port] */
public class JavalinServer {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 4000;
        var http = new GraphQLHttp(GraphQL.newGraphQL(Shelfie.schema(Path.of("../shelfie.graphqls"))).build());
        Javalin.create(config -> {
            config.concurrency.useVirtualThreads = true;
            // Every method goes to the handler: it, not the router, decides what 405 means.
            for (var method : List.of(HandlerType.GET, HandlerType.POST, HandlerType.PUT, HandlerType.PATCH, HandlerType.DELETE))
                config.routes.addHttpHandler(method, "/graphql", ctx -> serve(http, ctx));
        }).start(port);
    }

    static void serve(GraphQLHttp http, Context ctx) {
        var headers = new HashMap<String, String>();
        ctx.headerMap().forEach((k, v) -> headers.put(k.toLowerCase(), v));
        var r = http.handle(ctx.method().name(), ctx.queryString(), headers, ctx.bodyAsBytes());
        r.headers().forEach(ctx::header);
        ctx.status(r.status()).result(r.body());
    }
}
