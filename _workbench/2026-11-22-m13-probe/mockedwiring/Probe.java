import graphql.GraphQL;
import graphql.schema.idl.*;
public class Probe {
  public static void main(String[] a) throws Exception {
    String sdl = java.nio.file.Files.readString(java.nio.file.Path.of(a[0]));
    var reg = new SchemaParser().parse(sdl);
    var schema = new SchemaGenerator().makeExecutableSchema(reg, RuntimeWiring.newRuntimeWiring().wiringFactory(new MockedWiringFactory()).build());
    var g = GraphQL.newGraphQL(schema).build();
    System.out.println(g.execute(a[1]).toSpecification());
  }
}
