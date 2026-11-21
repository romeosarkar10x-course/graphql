package dev.shelfie.app;

import dev.shelfie.tiny.TinyGraphQL;
import java.util.HashMap;
import java.util.Map;

/** Shelfie on the tiny starter: no wiring code here, only annotated controllers. java -jar ... [port] */
public final class ShelfieApp {
    public static void main(String[] args) {
        TinyGraphQL.builder()
            .schema("/shelfie.graphqls")
            .scan("dev.shelfie.app")
            .context(headers -> {
                var ctx = new HashMap<String, Object>();
                if (headers.get("x-shelfie-user") != null) ctx.put("viewer", headers.get("x-shelfie-user"));
                return ctx;
            })
            .start(args.length > 0 ? Integer.parseInt(args[0]) : 4000);
    }
}
