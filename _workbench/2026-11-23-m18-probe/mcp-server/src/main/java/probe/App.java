package probe;

import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootApplication
public class App {
  public static void main(String[] args) { SpringApplication.run(App.class, args); }

  @Bean ToolCallbackProvider tools(ShelfieTools t) { return MethodToolCallbackProvider.builder().toolObjects(t).build(); }

  @Component
  public static class ShelfieTools {
    final HttpSyncGraphQlClient client = HttpSyncGraphQlClient.create(RestClient.create("http://shelfie-ref:8080/graphql"));

    @Tool(description = "Look up a book club by id: name, member count, current book.")
    public Object club(@ToolParam(description = "Club id, e.g. c1") String id, ToolContext ctx) {
      var attrs = RequestContextHolder.getRequestAttributes();
      String hdr = attrs instanceof ServletRequestAttributes s ? s.getRequest().getHeader("X-Shelfie-User") : "NO-REQUEST-CONTEXT";
      Map<String, Object> data = client.document("query($id: ID!) { club(id: $id) { name memberCount currentBook { title averageRating } } }")
          .variable("id", id).executeSync().toEntity(Map.class);
      return Map.of("data", data == null ? Map.of() : data, "header", String.valueOf(hdr),
          "ctxKeys", ctx == null ? "null" : ctx.getContext().keySet().toString(),
          "thread", Thread.currentThread().toString());
    }
  }
}
