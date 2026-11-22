import graphql.language.*;
import graphql.parser.Parser;

/** Does graphql-java's parser keep CR in block strings written with CRLF line endings? */
public class Crlf {
    public static void main(String[] a) {
        for (String src : new String[] {"{ f(s: \"\"\"one\r\ntwo\"\"\") }", "{ f(s: \"\"\"one\rtwo\"\"\") }", "{ f(s: \"\"\"\r\n  one\r\n  two\r\n\"\"\") }",
                "\"\"\"desc\r\nline\"\"\" type Q { a: Int }"}) {
            Document d = new Parser().parseDocument(src);
            String v = d.getDefinitions().get(0) instanceof OperationDefinition o
                    ? ((StringValue) ((Field) o.getSelectionSet().getSelections().get(0)).getArguments().get(0).getValue()).getValue()
                    : ((ObjectTypeDefinition) d.getDefinitions().get(0)).getDescription().getContent();
            System.out.println(src.replace("\r", "\\r").replace("\n", "\\n") + "  ->  " + v.replace("\r", "\\r").replace("\n", "\\n"));
        }
    }
}
