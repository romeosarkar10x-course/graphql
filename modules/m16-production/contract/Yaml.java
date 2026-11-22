import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * YAML to JSON for the tests (Module 16: Kubernetes manifests), so jq can read them.
 *   bin/course java modules/m16-production/contract/Yaml.java file.yaml    prints a JSON array, one item per document
 */
public class Yaml {
    public static void main(String[] a) throws Exception {
        YAMLMapper yaml = YAMLMapper.builder().build();
        List<JsonNode> docs = new ArrayList<>();
        try (var it = yaml.readValues(yaml.createParser(new File(a[0])), JsonNode.class)) {
            while (it.hasNext()) {
                JsonNode d = it.next();
                if (d != null && !d.isNull() && !d.isMissingNode()) docs.add(d);
            }
        }
        System.out.println(JsonMapper.builder().build().writeValueAsString(docs));
    }
}
