import graphql.schema.diff.*;
import graphql.schema.diff.reporting.CapturingReporter;
import java.nio.file.*;
public class Events {
  public static void main(String[] a) throws Exception {
    String old = Files.readString(Path.of(a[0]));
    for (int i = 1; i < a.length; i++) {
      var r = new CapturingReporter();
      new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(old, Files.readString(Path.of(a[i]))), r);
      System.out.println("== " + a[i]);
      for (var e : r.getEvents()) if (e.getLevel() != DiffLevel.INFO)
        System.out.println("  " + e.getLevel() + " " + e.getCategory() + " type=" + e.getTypeName() + " kind=" + e.getTypeKind() + " field=" + e.getFieldName() + " components=" + e.getComponents() + " | " + e.getReasonMsg());
    }
  }
}
