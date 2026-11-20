import graphql.parser.*; import graphql.language.*;
public class Probe {
  static void t(String label, String doc) {
    try { Document d = Parser.parse(doc); System.out.println("OK   " + label + " -> " + AstPrinter.printAstCompact(d)); }
    catch (Exception e) { System.out.println("ERR  " + label + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage()); }
  }
  public static void main(String[] a) {
    t("operation description", "\"Club card\" query Card { hello }");
    t("fragment arguments", "query { ...F(x: 1) } fragment F($x: Int = 0) on Query { hello }");
    t("directive on variable def", "query Q($a: Int @deprecated) { hello }");
    var opts = ParserOptions.newParserOptions().build();
    System.out.println("defaults: " + opts);
  }
}
