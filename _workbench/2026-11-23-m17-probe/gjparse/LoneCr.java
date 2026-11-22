import graphql.language.*;
import graphql.parser.Parser;

/** Line numbers graphql-java gives a field after LF, CRLF and a lone CR. */
public class LoneCr {
    public static void main(String[] a) {
        for (String nl : new String[] {"\n", "\r\n", "\r", " "}) {
            String src = "{" + nl + "a }";
            try {
                Field f = (Field) ((OperationDefinition) new Parser().parseDocument(src).getDefinitions().get(0)).getSelectionSet().getSelections().get(0);
                System.out.println(nl.codePoints().mapToObj(c -> String.format("U+%04X", c)).toList() + " -> a at " + f.getSourceLocation().getLine() + ":" + f.getSourceLocation().getColumn());
            } catch (Exception e) { System.out.println(nl + " -> " + e.getMessage()); }
        }
    }
}
