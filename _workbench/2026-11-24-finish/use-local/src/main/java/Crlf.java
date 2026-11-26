import graphql.language.*;
import graphql.parser.Parser;

public class Crlf {
    public static void main(String[] a) {
        Document d = Parser.parse("{ f(s: \"\"\"\r\n    one\r\n    two\r\n\"\"\") }");
        Field f = (Field) ((OperationDefinition) d.getDefinitions().get(0)).getSelectionSet().getSelections().get(0);
        String v = ((StringValue) f.getArguments().get(0).getValue()).getValue();
        System.out.println(graphql.GraphQL.class.getPackage().getImplementationVersion() + " " + v.replace("\r", "\\r").replace("\n", "\\n"));
    }
}
