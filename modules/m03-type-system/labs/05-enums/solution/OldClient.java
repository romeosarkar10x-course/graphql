import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/**
 * A client shipped against schema v1, decoding with Jackson's defaults: an unknown enum value is an
 * error. Given; do not edit. Reads a response file, prints one line per shelf entry.
 */
public class OldClient {
    enum ReadingStatus { WANT_TO_READ, READING, FINISHED }
    record Entry(String bookId, ReadingStatus status) {}
    record Data(List<Entry> shelf) {}
    record Response(Data data) {}

    public static void main(String[] args) throws Exception {
        Response r = JsonMapper.builder().build().readValue(Files.readString(Path.of(args[0])), Response.class);
        for (Entry e : r.data().shelf()) System.out.println(e.bookId() + " " + e.status());
    }
}
