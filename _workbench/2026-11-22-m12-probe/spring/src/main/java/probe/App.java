package probe;

import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.client.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
public class App {
  public static void main(String[] a) { SpringApplication.run(App.class, a); }
  record Book(String title, List<String> authors) {}
  record Owner(String name) {}
  record ClubDigest(String id, String name, int memberCount, Book currentBook, Owner owner) {}
  record Message(String id, String author, String body) {}

  final HttpSyncGraphQlClient sync; final HttpGraphQlClient reactive; final Deque<Message> recent = new ConcurrentLinkedDeque<>();
  App(@Value("${shelfie.url}") String url, @Value("${chat.url}") String chat) {
    sync = HttpSyncGraphQlClient.builder(RestClient.create(url))
        .interceptor(new SyncGraphQlClientInterceptor() {
          public ClientGraphQlResponse intercept(ClientGraphQlRequest r, Chain c) { System.out.println("sync intercept " + r.getDocument().length()); return c.next(r); }
        }).header("X-Shelfie-User", "u1").build();
    reactive = HttpGraphQlClient.create(WebClient.create(url));
    var ws = WebSocketGraphQlClient.builder(chat, new ReactorNettyWebSocketClient()).build();
    ws.document("subscription($r: ID!) { messageSent(roomId: $r) { id author body } }").variable("r", "r1")
      .retrieveSubscription("messageSent").toEntity(Message.class)
      .doOnSubscribe(s -> System.out.println("ws subscribed"))
      .subscribe(recent::add, e -> System.out.println("ws error " + e));
  }
  @GetMapping("/clubs/{id}") ClubDigest club(@PathVariable String id) {
    return sync.documentName("clubDigest").variable("id", id).retrieveSync("club").toEntity(ClubDigest.class);
  }
  @GetMapping("/clubs/{id}/reactive") Mono<ClubDigest> clubR(@PathVariable String id) {
    return reactive.documentName("clubDigest").variable("id", id).retrieve("club").toEntity(ClubDigest.class);
  }
  @GetMapping("/bad") Object bad() { return sync.document("{ club(id: \"c1\") { nope } }").retrieveSync("club").toEntity(Map.class); }
  @GetMapping("/chat") Collection<Message> chat() { return recent; }
}
