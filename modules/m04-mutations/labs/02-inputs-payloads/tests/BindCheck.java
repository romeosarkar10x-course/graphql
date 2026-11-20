import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Runs Bind.bind over tests/bind-cases.json and prints one line per case. Compiled with the learner's files. */
public class BindCheck {
    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        var json = JsonMapper.builder().build();
        for (Object o : json.readValue(Files.readString(Path.of(a[0])), List.class)) {
            Map<String, Object> c = (Map<String, Object>) o;
            String name = (String) c.get("record");
            Class<? extends Record> type = (Class<? extends Record>) Class.forName("Inputs$" + name);
            try {
                System.out.println(c.get("case") + " " + Bind.bind((Map<String, Object>) c.get("input"), type));
            } catch (RuntimeException e) {
                System.out.println(c.get("case") + " error " + e.getClass().getSimpleName());
            }
        }
    }
}
