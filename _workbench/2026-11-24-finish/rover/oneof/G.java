import graphql.schema.idl.*;
import java.nio.file.*;
public class G { public static void main(String[] a) throws Exception {
  var reg = new SchemaParser().parse(Files.readString(Path.of(a[0])));
  var s = new SchemaGenerator().makeExecutableSchema(reg, RuntimeWiring.MOCKED_WIRING);
  System.out.println("graphql-java ok, ToolLookup isOneOf=" + ((graphql.schema.GraphQLInputObjectType) s.getType("ToolLookup")).isOneOf());
}}
