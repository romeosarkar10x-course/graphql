package dev.shelfie.cli;

import java.util.List;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import tools.jackson.databind.JsonNode;

/**
 * Mini-project 12.10: the Shelfie CLI.
 *   java -jar target/shelfie-cli.jar <url>                    interactive: tab completion, history, multi-line
 *   java -jar target/shelfie-cli.jar <url> -e '<document>'    run one document, print data as JSON
 *   java -jar target/shelfie-cli.jar <url> --complete '<text>' print the completions for the end of <text>
 * Today -e works and the interactive mode runs each line as it is typed, with no completion.
 */
public class Main {
  public static void main(String[] args) throws Exception {
    Http http = new Http(args.length > 0 ? args[0] : "http://shelfie-ref:8080/graphql");
    if (args.length > 2 && args[1].equals("-e")) System.exit(run(http, args[2], System.out::println, System.err::println));
    if (args.length > 2 && args[1].equals("--complete")) {
      complete(args[2]).forEach(System.out::println);
      return;
    }
    interactive(http);
  }

  /** TODO: the completions for the end of text, from the schema (introspection), sorted. */
  static List<String> complete(String text) {
    return List.of();
  }

  /** Prints data (pretty JSON); errors go to err and make the exit code 1. */
  static int run(Http http, String document, java.util.function.Consumer<String> out, java.util.function.Consumer<String> err) {
    JsonNode r = http.execute(document);
    if (r.has("data") && !r.get("data").isNull()) out.accept(Http.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(r.get("data")));
    for (JsonNode e : r.path("errors")) err.accept("error: " + e.path("message").asString());
    return r.path("errors").isEmpty() ? 0 : 1;
  }

  static void interactive(Http http) throws Exception {
    try (Terminal terminal = TerminalBuilder.builder().system(true).dumb(true).build()) {
      LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
      var out = terminal.writer();
      while (true) {
        String line;
        try {
          line = reader.readLine("shelfie> ");
        } catch (UserInterruptException e) {
          continue;
        } catch (EndOfFileException e) {
          break;
        }
        if (line.trim().equals(":quit")) break;
        if (!line.isBlank()) run(http, line, out::println, out::println);
        out.flush();
      }
    }
  }
}
