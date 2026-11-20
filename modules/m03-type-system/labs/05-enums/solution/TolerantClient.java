import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The same v1 client, made tolerant: values it does not know become UNKNOWN, and it says so.
 * Reads a response file, prints one line per shelf entry.
 */
public class TolerantClient {
    enum ReadingStatus { WANT_TO_READ, READING, FINISHED, @JsonEnumDefaultValue UNKNOWN }
    record Entry(String bookId, ReadingStatus status) {}
    record Data(List<Entry> shelf) {}
    record Response(Data data) {}

    public static void main(String[] args) throws Exception {
        JsonMapper json = JsonMapper.builder().enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE).build();
        Response r = json.readValue(Files.readString(Path.of(args[0])), Response.class);
        for (Entry e : r.data().shelf()) System.out.println(e.bookId() + " " + e.status());
        long unknown = r.data().shelf().stream().filter(e -> e.status() == ReadingStatus.UNKNOWN).count();
        if (unknown > 0) System.err.println("warning: " + unknown + " status value(s) this client does not know; upgrade the client");
    }
}
