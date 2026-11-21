// The client build's last step (lab 10.5): every operation in a folder becomes a trusted document.
//   bin/course java client/BuildManifest.java client/operations src/main/resources/trusted-documents.json
// Format: Apollo's persisted query manifest. id = SHA-256 of body; body = the file's text, trimmed.
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import tools.jackson.databind.json.JsonMapper;

public class BuildManifest {
  public static void main(String[] args) throws Exception {
    List<Map<String, Object>> ops = new ArrayList<>();
    try (var files = Files.list(Path.of(args[0]))) {
      for (Path f : files.filter(p -> p.toString().endsWith(".graphql")).sorted().toList()) {
        String body = Files.readString(f).strip();
        Matcher m = Pattern.compile("^(query|mutation|subscription)\\s+(\\w+)").matcher(body);
        if (!m.find()) throw new IllegalArgumentException(f + ": every trusted document needs a named operation");
        var op = new LinkedHashMap<String, Object>();
        op.put("id", sha256(body));
        op.put("name", m.group(2));
        op.put("type", m.group(1));
        op.put("body", body);
        ops.add(op);
      }
    }
    var manifest = new LinkedHashMap<String, Object>();
    manifest.put("format", "apollo-persisted-query-manifest");
    manifest.put("version", 1);
    manifest.put("operations", ops);
    Files.writeString(Path.of(args[1]), JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n");
    System.out.println(ops.size() + " trusted documents in " + args[1]);
  }

  static String sha256(String s) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
