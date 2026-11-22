package dev.shelfie.cli;

import java.nio.file.Path;
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
 */
public class Main {
  public static void main(String[] args) throws Exception {
    Http http = new Http(args.length > 0 ? args[0] : "http://shelfie-ref:8080/graphql");
    if (args.length > 2 && args[1].equals("-e")) System.exit(run(http, args[2], System.out::println, System.err::println));
    Schema schema = Schema.load(http);
    if (args.length > 2 && args[1].equals("--complete")) {
      Completion.complete(schema, args[2]).forEach(System.out::println);
      return;
    }
    interactive(http, schema);
  }

  /** Prints data (pretty JSON); errors go to err and make the exit code 1. */
  static int run(Http http, String document, java.util.function.Consumer<String> out, java.util.function.Consumer<String> err) {
    JsonNode r = http.execute(document);
    if (r.has("data") && !r.get("data").isNull()) out.accept(Http.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(r.get("data")));
    for (JsonNode e : r.path("errors")) err.accept("error: " + e.path("message").asString());
    return r.path("errors").isEmpty() ? 0 : 1;
  }

  static void interactive(Http http, Schema schema) throws Exception {
    StringBuilder pending = new StringBuilder();
    try (Terminal terminal = TerminalBuilder.builder().system(true).dumb(true).build()) {
      LineReader reader = LineReaderBuilder.builder()
          .terminal(terminal)
          .completer(new ShelfieCompleter(schema, pending))
          .variable(LineReader.HISTORY_FILE, Path.of(System.getProperty("user.home"), ".shelfie_history"))
          .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)   // ! in a document is not history
          .build();
      var out = terminal.writer();
      out.println("Shelfie CLI: Tab completes from the schema, :schema <Type> describes a type, :quit leaves.");
      while (true) {
        String line;
        try {
          line = reader.readLine(pending.isEmpty() ? "shelfie> " : "   ...> ");
        } catch (UserInterruptException e) {
          pending.setLength(0);
          continue;
        } catch (EndOfFileException e) {
          break;
        }
        if (pending.isEmpty() && line.isBlank()) continue;
        if (pending.isEmpty() && line.startsWith(":")) {
          if (!command(line.trim(), schema, out)) break;
          continue;
        }
        pending.append(line).append('\n');
        if (balanced(pending)) {
          run(http, pending.toString(), out::println, s -> out.println(s));
          out.flush();
          pending.setLength(0);
        }
      }
    }
  }

  static boolean command(String line, Schema schema, java.io.PrintWriter out) {
    String[] parts = line.split("\\s+");
    switch (parts[0]) {
      case ":quit", ":q" -> { return false; }
      case ":schema" -> {
        Schema.TypeInfo t = parts.length > 1 ? schema.type(parts[1]) : schema.type(schema.query);
        if (t == null) out.println("no type " + (parts.length > 1 ? parts[1] : ""));
        else if (!t.fields().isEmpty()) t.fields().values().forEach(f -> out.println("  " + f.name()
            + (f.args().isEmpty() ? "" : "(" + String.join(", ", f.args().stream().map(a -> a.name() + ": " + a.type()).toList()) + ")")
            + ": " + f.type()));
        else if (!t.possibleTypes().isEmpty()) out.println("  " + t.kind() + " of " + String.join(" | ", t.possibleTypes()));
        else out.println("  " + t.kind());
      }
      default -> out.println("commands: :schema [Type], :quit");
    }
    out.flush();
    return true;
  }

  /** A document is complete when every { has its } (outside strings) and it has at least one. */
  static boolean balanced(CharSequence text) {
    int depth = 0, opened = 0;
    boolean inString = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (inString) { if (c == '\\') i++; else if (c == '"') inString = false; continue; }
      if (c == '"') inString = true;
      else if (c == '{') { depth++; opened++; }
      else if (c == '}') depth--;
    }
    return opened > 0 && depth <= 0;
  }
}
