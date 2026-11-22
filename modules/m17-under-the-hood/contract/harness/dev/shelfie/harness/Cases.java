package dev.shelfie.harness;

import dev.shelfie.mini.Ast;
import dev.shelfie.mini.Parser;
import dev.shelfie.mini.SyntaxError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed cases for labs 17.2 and 17.3, mostly where graphql-java 26.1 departs from the September 2025 spec (so the
 * differential tests skip them): your engine must follow the spec. Prints "pass" or "FAIL" per case.
 *
 *   Cases lexer | parser | --print lexer|parser   (--print shows what your engine gives, for authoring)
 */
public final class Cases {
    /** source -> expected tokens ("Kind value @line:column", joined by " | "), or "REJECT". */
    static final Map<String, String> LEXER = new LinkedHashMap<>();
    /** source -> what to check: "REJECT", "ACCEPT", or a substring of the AST's toString(). */
    static final Map<String, String> PARSER = new LinkedHashMap<>();

    static {
        LEXER.put("{ a }", "Punct { @1:1 | Name a @1:3 | Punct } @1:5");
        LEXER.put("\uFEFF{\n  a\r\n  b\r  c }", "Punct { @1:2 | Name a @2:3 | Name b @3:3 | Name c @4:3 | Punct } @4:5");
        LEXER.put("{ a\u2028b }", "REJECT");
        LEXER.put("{ f(s: \"\\u{1F600}\\uD83D\\uDE00\\u00e9\") }", "Punct { @1:1 | Name f @1:3 | Punct ( @1:4 | Name s @1:5 | Punct : @1:6 | String \uD83D\uDE00\uD83D\uDE00\u00e9 @1:8 | Punct ) @1:37 | Punct } @1:39");
        LEXER.put("{ f(s: \"\\uD83D\") }", "REJECT");
        LEXER.put("{ f(s: \"\\u{110000}\") }", "REJECT");
        LEXER.put("{ f(s: \"\uD83D\uDE00\") g }", "Punct { @1:1 | Name f @1:3 | Punct ( @1:4 | Name s @1:5 | Punct : @1:6 | String \uD83D\uDE00 @1:8 | Punct ) @1:11 | Name g @1:13 | Punct } @1:15");
        LEXER.put("{ f(s: \"\"\"\r\n    one\r\n      two\r\n  \"\"\") }", "Punct { @1:1 | Name f @1:3 | Punct ( @1:4 | Name s @1:5 | Punct : @1:6 | String one\n  two @1:8 | Punct ) @4:6 | Punct } @4:8");
        LEXER.put("{ f(s: \"\"\"\n  a\n \n  b\n\"\"\") }", "Punct { @1:1 | Name f @1:3 | Punct ( @1:4 | Name s @1:5 | Punct : @1:6 | String a\n\nb @1:8 | Punct ) @5:4 | Punct } @5:6");
        LEXER.put("{ f(s: \"\"\" \\\"\"\" \"\"\") }", "Punct { @1:1 | Name f @1:3 | Punct ( @1:4 | Name s @1:5 | Punct : @1:6 | String  \"\"\"  @1:8 | Punct ) @1:20 | Punct } @1:22");
        LEXER.put("{ f(n: 0x1) }", "REJECT");
        LEXER.put("{ f(n: 00) }", "REJECT");
        LEXER.put("{ f(n: 1.) }", "REJECT");
        LEXER.put("{ f(n: 1e) }", "REJECT");
        LEXER.put("{ f(n: 1.5e+3, m: -0) }", "Punct { @1:1 | Name f @1:3 | Punct ( @1:4 | Name n @1:5 | Punct : @1:6 | Float 1.5e+3 @1:8 | Name m @1:16 | Punct : @1:17 | Int -0 @1:19 | Punct ) @1:21 | Punct } @1:23");
        LEXER.put("{ a.b }", "REJECT");
        LEXER.put("{ ...F }", "Punct { @1:1 | Punct ... @1:3 | Name F @1:6 | Punct } @1:8");
        LEXER.put("{ a # comment \"not a string\n b }", "Punct { @1:1 | Name a @1:3 | Name b @2:2 | Punct } @2:4");
        LEXER.put("{ f(s: \"tab\there\") }", "Punct { @1:1 | Name f @1:3 | Punct ( @1:4 | Name s @1:5 | Punct : @1:6 | String tab\there @1:8 | Punct ) @1:18 | Punct } @1:20");
        LEXER.put("{ f(s: \"open) }", "REJECT");

        PARSER.put("\"Lists the clubs.\" query Clubs(\"How many.\" $n: Int) { clubs { id } }", "description=Lists the clubs., type=query, name=Clubs, variables=[VariableDef[loc=Loc[line=1, column=32], description=How many.");
        PARSER.put("\"\"\"A fragment.\"\"\" fragment F on Club { id } { clubs { ...F } }", "Fragment[loc=Loc[line=1, column=1], description=A fragment., name=F");
        PARSER.put("{ a { } }", "REJECT");
        PARSER.put("type T {}", "REJECT");
        PARSER.put("enum E {}", "REJECT");
        PARSER.put("type T { u @s }", "REJECT");
        PARSER.put("type T", "ObjectDef[description=null, name=T, interfaces=[], directives=[], fields=[]]");
        PARSER.put("extend scalar S", "REJECT");
        PARSER.put("extend scalar S @tag", "Extension[definition=ScalarDef[description=null, name=S, directives=[Directive[");
        PARSER.put("extend type T implements & A & B", "Extension[definition=ObjectDef[description=null, name=T, interfaces=[A, B]");
        PARSER.put("\"no\" extend type T @x", "REJECT");
        PARSER.put("fragment on on T { a }", "REJECT");
        PARSER.put("{ ... on T { a } ... @x { b } }", "Inline[loc=Loc[line=1, column=3], typeCondition=T");
        PARSER.put("{ ...on }", "REJECT");
        PARSER.put("query Q($v: [Int!]! = [1, 2] @d) { f(a: $v, b: {c: [$v]}) }", "type=NonNullType[of=ListType[of=NonNullType[of=NamedType[name=Int]]]], defaultValue=ListV[values=[IntV[text=1], IntV[text=2]]]");
        PARSER.put("query Q($v: Int = $w) { f }", "REJECT");
        PARSER.put("directive @d(a: Int = 1) repeatable on | FIELD | QUERY", "DirectiveDef[description=null, name=d, arguments=[InputValueDef[description=null, name=a, type=NamedType[name=Int], defaultValue=IntV[text=1], directives=[]]], repeatable=true, locations=[FIELD, QUERY]]");
        PARSER.put("union U = | A | B", "UnionDef[description=null, name=U, directives=[], members=[A, B]]");
        PARSER.put("enum E { true }", "REJECT");
        PARSER.put("schema { query: Q } extend schema @x", "Extension[definition=SchemaDef[description=null, directives=[Directive[");
        PARSER.put("", "REJECT");
    }

    public static void main(String[] a) {
        boolean print = a[0].equals("--print");
        String which = print ? a[1] : a[0];
        int passed = 0, total = 0;
        for (var e : (which.equals("lexer") ? LEXER : PARSER).entrySet()) {
            total++;
            String got = which.equals("lexer") ? lex(e.getKey()) : parse(e.getKey());
            boolean ok = which.equals("lexer") ? got.equals(e.getValue())
                    : e.getValue().equals("REJECT") ? got.equals("REJECT") : !got.equals("REJECT") && !got.startsWith("CRASH") && got.contains(e.getValue());
            if (print) { System.out.println(TokenDiff.esc(e.getKey()) + "\n    -> " + TokenDiff.esc(got)); continue; }
            if (ok) passed++;
            System.out.println((ok ? "pass " : "FAIL ") + TokenDiff.esc(e.getKey()) + (ok ? "" : "\n      expected " + TokenDiff.esc(e.getValue()) + "\n      got      " + TokenDiff.esc(got)));
        }
        if (!print) System.out.println("passed " + passed + " of " + total);
    }

    static String lex(String src) {
        try { return String.join(" | ", TokenDiff.ours(src)); }
        catch (SyntaxError e) { return "REJECT"; }
        catch (RuntimeException e) { return "CRASH " + e; }
    }

    static String parse(String src) {
        try { Ast.Document d = Parser.parse(src); return d.toString(); }
        catch (SyntaxError e) { return "REJECT"; }
        catch (RuntimeException e) { return "CRASH " + e; }
    }
}
