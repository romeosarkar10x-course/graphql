import graphql.*; import graphql.schema.idl.*; import java.nio.file.*;
public class Validate {
  public static void main(String[] a) throws Exception {
    var reg = new SchemaParser().parse(Files.readString(Path.of("ref-schema.graphqls")));
    var schema = new SchemaGenerator().makeExecutableSchema(reg, RuntimeWiring.MOCKED_WIRING);
    var gql = GraphQL.newGraphQL(schema).build();
    for (String q : a) {
      var r = gql.execute(ExecutionInput.newExecutionInput(q).build());
      System.out.println(q + "\n  -> " + r.getErrors());
    }
  }
}
