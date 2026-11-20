import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
public class E {
  enum S { A, B, @JsonEnumDefaultValue UNKNOWN }
  enum T { A, B }
  record R(S s) {}
  record Q(T s) {}
  public static void main(String[] a) {
    try { System.out.println("strict: " + JsonMapper.builder().build().readValue("{\"s\":\"C\"}", Q.class)); } catch (Exception e) { System.out.println("strict: " + e.getClass().getSimpleName() + ": " + e.getMessage().lines().findFirst().get()); }
    System.out.println("default-value: " + JsonMapper.builder().enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE).build().readValue("{\"s\":\"C\"}", R.class));
    System.out.println("as-null: " + JsonMapper.builder().enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL).build().readValue("{\"s\":\"C\"}", Q.class));
  }
}
