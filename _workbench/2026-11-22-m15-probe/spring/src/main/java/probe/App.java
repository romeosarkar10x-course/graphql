package probe;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.dataloader.DataLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.federation.FederationSchemaFactory;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class App {
    public static void main(String[] a) { SpringApplication.run(App.class, a); }

    @Bean GraphQlSourceBuilderCustomizer fed(FederationSchemaFactory f) { return b -> b.schemaFactory(f::createGraphQLSchema); }
    @Bean FederationSchemaFactory factory() { return new FederationSchemaFactory(); }

    static final AtomicInteger entityCalls = new AtomicInteger(), entityItems = new AtomicInteger(), httpCalls = new AtomicInteger();

    @RestController static class Stats {
        @GetMapping("/_stats") Map<String,Integer> s() { return Map.of("entityCalls", entityCalls.get(), "entityItems", entityItems.get()); }
        @PostMapping("/_reset") void r() { entityCalls.set(0); entityItems.set(0); }
    }

    record Book(String id, String title, int pages) {}
    record Review(String id, String bookId, String authorId, int rating, String body) {}
    record User(String id, String name, String handle) {}

    static final Map<String, Book> BOOKS = new LinkedHashMap<>();
    static final List<Review> REVIEWS = List.of(new Review("r1","b1","u1",5,"x"), new Review("r2","b1","u2",4,"y"), new Review("r6","b2","u1",5,"z"), new Review("r8","b4","u7",5,"w"));
    static { for (var b : List.of(new Book("b1","The Left Hand of Darkness",304), new Book("b2","Piranesi",272), new Book("b4","The Name of the Rose",536))) BOOKS.put(b.id(), b); }

    @Controller @Profile("catalog-single") static class CatalogSingle {
        @QueryMapping List<Book> books() { return List.copyOf(BOOKS.values()); }
        @EntityMapping Book book(@Argument String id) { entityCalls.incrementAndGet(); entityItems.incrementAndGet(); return BOOKS.get(id); }
    }
    @Controller @Profile("catalog-list") static class CatalogList {
        @QueryMapping List<Book> books() { return List.copyOf(BOOKS.values()); }
        @EntityMapping List<Book> book(@Argument List<String> idList) { entityCalls.incrementAndGet(); entityItems.addAndGet(idList.size()); return idList.stream().map(BOOKS::get).toList(); }
    }
    @Controller @Profile("catalog-loader") static class CatalogLoader {
        CatalogLoader(BatchLoaderRegistry r) { r.forTypePair(String.class, Book.class).registerBatchLoader((ids, env) -> { entityCalls.incrementAndGet(); entityItems.addAndGet(ids.size()); return reactor.core.publisher.Flux.fromIterable(ids).map(BOOKS::get); }); }
        @QueryMapping List<Book> books() { return List.copyOf(BOOKS.values()); }
        @EntityMapping CompletableFuture<Book> book(@Argument String id, DataLoader<String, Book> loader) { return loader.load(id); }
    }
    @Controller @Profile("reviews") static class Reviews {
        @QueryMapping List<Review> reviews() { return REVIEWS; }
        @SchemaMapping(typeName="Review") Map<String,Object> book(Review r) { return Map.of("id", r.bookId()); }
        @SchemaMapping(typeName="Review") Map<String,Object> author(Review r) { return Map.of("id", r.authorId()); }
        @EntityMapping Map<String,Object> book(@Argument("id") String id, Map<String,Object> rep) { return rep; }
        @EntityMapping Review review(@Argument String id) { return REVIEWS.stream().filter(r -> r.id().equals(id)).findFirst().orElse(null); }
        @SchemaMapping(typeName="Book") List<Review> reviews(Map<String,Object> book) { return REVIEWS.stream().filter(r -> r.bookId().equals(book.get("id"))).toList(); }
    }
}
