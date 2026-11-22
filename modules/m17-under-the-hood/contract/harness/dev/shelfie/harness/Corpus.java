package dev.shelfie.harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The differential corpus: every distinct .graphql and .graphqls file under the course's modules/ and reference/
 * (up to 300, sorted by path), then documents generated from the grammar with a fixed seed, up to the count asked.
 * Generated documents lean on the lexer's corners: escapes, block-string indentation, numbers, comments, commas,
 * CRLF, tabs and a byte order mark.
 *
 *   java dev.shelfie.harness.Corpus <count> <seed> <dir>   writes the corpus as files 0000.graphql ...
 */
public final class Corpus {
    public static void main(String[] a) throws IOException {
        List<String> docs = documents(Integer.parseInt(a[0]), Long.parseLong(a[1]));
        Path dir = Path.of(a[2]);
        Files.createDirectories(dir);
        for (int i = 0; i < docs.size(); i++) Files.writeString(dir.resolve(String.format("%04d.graphql", i)), docs.get(i));
        System.out.println(docs.size() + " documents in " + dir);
    }

    public static List<String> documents(int count, long seed) throws IOException {
        Set<String> docs = new LinkedHashSet<>();
        List<Path> files = new ArrayList<>();
        for (String root : List.of("/course/modules", "/course/reference"))
            try (Stream<Path> s = Files.walk(Path.of(root))) {
                s.filter(p -> p.toString().matches(".*\\.graphqls?$") && !p.toString().contains("/target/") && !p.toString().contains("node_modules"))
                        .forEach(files::add);
            }
        files.sort(null);
        for (Path p : files) if (docs.size() < Math.min(300, count)) docs.add(Files.readString(p));
        Gen g = new Gen(new Random(seed));
        while (docs.size() < count) docs.add(g.document());
        return new ArrayList<>(docs);
    }

    /** A random document: mostly well-formed operations and SDL, written with varied lexical choices. */
    static final class Gen {
        final Random r;
        final StringBuilder sb = new StringBuilder();
        Gen(Random r) { this.r = r; }

        String document() {
            sb.setLength(0);
            if (r.nextInt(20) == 0) sb.append('﻿');
            int n = 1 + r.nextInt(3);
            for (int i = 0; i < n; i++) { if (r.nextBoolean()) operation(); else if (r.nextInt(3) == 0) fragment(); else typeDef(); sep(); }
            return r.nextInt(4) == 0 ? sb.toString().replace("\n", "\r\n") : sb.toString();
        }

        void sep() {
            switch (r.nextInt(9)) {
                case 0 -> sb.append(", ");
                case 1 -> sb.append('\t');
                case 2 -> sb.append("\n");
                case 3 -> sb.append(" # ").append(word()).append(" ünïcødé ✓\n");
                case 4 -> sb.append(",,\n  ");
                default -> sb.append(' ');
            }
        }

        String word() {
            String[] words = {"id", "title", "book", "club", "members", "reviews", "on", "query", "type", "true", "null", "fragment", "extend", "_x", "a1", "B_2", "rating"};
            return words[r.nextInt(words.length)];
        }

        void operation() {
            if (r.nextInt(5) == 0) { selectionSet(0); return; }
            sb.append(new String[] {"query", "mutation", "subscription"}[r.nextInt(3)]);
            if (r.nextBoolean()) sb.append(' ').append(word().equals("on") ? "Op" : word()).append(r.nextInt(9));
            if (r.nextBoolean()) {
                sb.append('(');
                int vars = 1 + r.nextInt(3);
                for (int i = 0; i < vars; i++) { sb.append("$v").append(i).append(':'); sep(); type(); if (r.nextBoolean()) { sb.append(" = "); value(1, true); } sep(); }
                sb.append(')');
            }
            directives();
            sep();
            selectionSet(0);
        }

        void fragment() {
            sb.append("fragment F").append(r.nextInt(99)).append(" on ").append(Character.toUpperCase(word().charAt(0))).append("T");
            directives();
            sep();
            selectionSet(0);
        }

        void selectionSet(int depth) {
            sb.append('{');
            int n = 1 + r.nextInt(4);
            for (int i = 0; i < n; i++) {
                sep();
                int k = r.nextInt(10);
                if (k == 0 && depth < 3) { sb.append("... on T"); directives(); sep(); selectionSet(depth + 1); }
                else if (k == 1) { sb.append("...F").append(r.nextInt(9)); directives(); }
                else if (k == 2 && depth < 3) { sb.append("... "); directives(); selectionSet(depth + 1); }
                else {
                    if (r.nextInt(4) == 0) sb.append(word()).append(": ");
                    sb.append(word());
                    if (r.nextInt(3) == 0) arguments(depth);
                    directives();
                    if (r.nextInt(3) == 0 && depth < 3) { sep(); selectionSet(depth + 1); }
                }
            }
            sep();
            sb.append('}');
        }

        void arguments(int depth) {
            sb.append('(');
            int n = 1 + r.nextInt(3);
            for (int i = 0; i < n; i++) { sb.append(word()).append(':'); sep(); value(depth, false); sep(); }
            sb.append(')');
        }

        void directives() {
            while (r.nextInt(6) == 0) {
                sb.append(" @").append(new String[] {"include", "skip", "deprecated", "x"}[r.nextInt(4)]);
                if (r.nextBoolean()) { sb.append("(if: "); value(2, false); sb.append(')'); }
            }
        }

        void value(int depth, boolean constant) {
            switch (r.nextInt(depth > 2 ? 8 : 10)) {
                case 0 -> sb.append(new String[] {"0", "-0", "7", "-12", "2147483647", "9007199254740993", "123456789012345678901234567890"}[r.nextInt(7)]);
                case 1 -> sb.append(new String[] {"1.5", "-0.0", "1e10", "1E-3", "6.02e+23", "0.1e1", "1.7976931348623157e308"}[r.nextInt(7)]);
                case 2 -> string();
                case 3 -> blockString();
                case 4 -> sb.append(new String[] {"true", "false", "null", "RED", "on", "query"}[r.nextInt(6)]);
                case 5 -> { if (constant) sb.append("ENUM_V"); else sb.append("$v").append(r.nextInt(3)); }
                case 6 -> string();
                case 7 -> sb.append("\"\"");
                case 8 -> { sb.append('['); int n = r.nextInt(3); for (int i = 0; i < n; i++) { value(depth + 1, constant); sep(); } sb.append(']'); }
                default -> { sb.append('{'); int n = r.nextInt(3); for (int i = 0; i < n; i++) { sb.append(word()).append(": "); value(depth + 1, constant); sep(); } sb.append('}'); }
            }
        }

        void string() {
            sb.append('"');
            int n = r.nextInt(6);
            for (int i = 0; i < n; i++)
                sb.append(new String[] {"plain", " ", "\\n", "\\\"", "\\\\", "\\/", "\\b\\f\\r\\t", "\\u00e9", "\\u{1F600}", "\\uD83D\\uDE00", "\\u{0}",
                        "\\u0041", "é", "日本", "😀", "\t", "#not a comment", "\\u{10FFFF}", "'", "\\u{00000041}"}[r.nextInt(20)]);
            sb.append('"');
        }

        void blockString() {
            sb.append("\"\"\"");
            String indent = new String[] {"", "  ", "    ", "\t"}[r.nextInt(4)];
            if (r.nextBoolean()) sb.append("\n");
            int n = 1 + r.nextInt(4);
            for (int i = 0; i < n; i++) {
                sb.append(i == 0 && r.nextBoolean() ? "" : indent).append(new String[] {"line", "  more", "\\\"\"\" quoted", "\"quotes\"", "", "   ", "\\n stays", "é ✓"}[r.nextInt(8)]);
                if (i < n - 1) sb.append(r.nextInt(5) == 0 ? "\r\n" : "\n");
            }
            if (r.nextBoolean()) sb.append("\n").append(indent);
            sb.append("\"\"\"");
        }

        void type() {
            String t = new String[] {"Int", "String", "ID", "Boolean", "Float", "BookInput"}[r.nextInt(6)];
            switch (r.nextInt(5)) {
                case 0 -> sb.append('[').append(t).append(']');
                case 1 -> sb.append('[').append(t).append("!]!");
                case 2 -> sb.append(t).append('!');
                default -> sb.append(t);
            }
        }

        void typeDef() {
            if (r.nextBoolean()) { if (r.nextBoolean()) string(); else blockString(); sb.append('\n'); }
            switch (r.nextInt(6)) {
                case 0 -> { sb.append("type T").append(r.nextInt(9)); if (r.nextBoolean()) sb.append(" implements & Node & Named"); directivesConst(); fields(); }
                case 1 -> { sb.append("interface Node"); fields(); }
                case 2 -> sb.append("union U = | A | B | C");
                case 3 -> { sb.append("enum E {"); for (int i = 0; i < 1 + r.nextInt(3); i++) { sep(); if (r.nextBoolean()) { string(); sb.append(' '); } sb.append("V").append(i); } sb.append(" }"); }
                case 4 -> { sb.append("input BookInput {"); for (int i = 0; i < 1 + r.nextInt(3); i++) { sep(); sb.append("f").append(i).append(": "); type(); if (r.nextBoolean()) { sb.append(" = "); value(1, true); } } sb.append(" }"); }
                default -> { sb.append("directive @d").append(r.nextInt(9)).append("(a: Int = 1)"); if (r.nextBoolean()) sb.append(" repeatable"); sb.append(" on | FIELD | QUERY"); }
            }
        }

        void directivesConst() { if (r.nextInt(3) == 0) { sb.append(" @key(fields: "); string(); sb.append(')'); } }

        void fields() {
            sb.append(" {");
            for (int i = 0; i < 1 + r.nextInt(4); i++) {
                sep();
                if (r.nextInt(3) == 0) { string(); sb.append(' '); }
                sb.append(word().equals("on") ? "onField" : word()).append(i);
                if (r.nextBoolean()) { sb.append("(first: Int = 10, after: String)"); }
                sb.append(": ");
                type();
                if (r.nextInt(4) == 0) sb.append(" @deprecated(reason: \"use x\")");
            }
            sep();
            sb.append('}');
        }
    }
}
