package dev.toolshed.client;

import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * The web UI's server: ui/index.html at /, the persisted documents manifest, and POST /graphql passed to the router as it is (same origin, so
 * the router needs no CORS rule). JDK only. COURSE_PUBLISH=4005 bin/course shell to open it from the host.
 */
class Ui {
    static void serve(int port, String router) throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", ex -> {
            try (InputStream in = Ui.class.getResourceAsStream("/static/index.html")) {
                byte[] page = in.readAllBytes();
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                ex.sendResponseHeaders(200, page.length);
                ex.getResponseBody().write(page);
            }
            ex.close();
        });
        // the page finds each operation's id here, as the Java client does
        server.createContext("/persisted-documents.json", ex -> {
            try (InputStream in = Ui.class.getResourceAsStream("/persisted-documents.json")) {
                byte[] body = in.readAllBytes();
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
            }
            ex.close();
        });
        server.createContext("/graphql", ex -> {
            try {
                HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(router))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(ex.getRequestBody().readAllBytes()))
                        .header("Content-Type", "application/json");
                String accept = ex.getRequestHeaders().getFirst("Accept");
                if (accept != null) req.header("Accept", accept);
                String auth = ex.getRequestHeaders().getFirst("Authorization");
                if (auth != null) req.header("Authorization", auth);
                HttpResponse<InputStream> res = http.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
                ex.getResponseHeaders().set("Content-Type", res.headers().firstValue("Content-Type").orElse("application/json"));
                ex.sendResponseHeaders(res.statusCode(), 0);
                try (InputStream body = res.body()) {
                    body.transferTo(ex.getResponseBody());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                ex.close();
            }
        });
        server.start();
        System.out.println("Toolshed UI on http://localhost:" + port + "/ (router " + router + ")");
    }
}
