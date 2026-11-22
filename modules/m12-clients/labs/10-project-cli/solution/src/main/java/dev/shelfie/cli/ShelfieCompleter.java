package dev.shelfie.cli;

import java.util.List;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/** JLine's hook: everything before the cursor, in this line and the lines of the document typed so far. */
final class ShelfieCompleter implements Completer {
  private final Schema schema;
  private final StringBuilder earlierLines;

  ShelfieCompleter(Schema schema, StringBuilder earlierLines) {
    this.schema = schema;
    this.earlierLines = earlierLines;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String before = earlierLines + line.line().substring(0, line.cursor());
    for (String c : Completion.complete(schema, before)) candidates.add(new Candidate(c));
  }
}
