package dev.shelfie.mini;

import dev.shelfie.mini.Ast.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A recursive-descent parser for the whole grammar (spec appendix C): executable and type system documents. One
 * method per production; one token of lookahead, two where a description may start a definition.
 */
public final class Parser {
    private final List<Token> tokens;
    private int pos;

    private Parser(String source) { tokens = Lexer.lex(source); }

    public static Document parse(String source) {
        Parser p = new Parser(source);
        List<Definition> defs = new ArrayList<>();
        do { defs.add(p.definition()); } while (!(p.peek() instanceof Token.End));
        return new Document(defs);
    }

    // ---- token helpers

    private Token peek() { return tokens.get(pos); }
    private Token peek(int ahead) { return tokens.get(Math.min(pos + ahead, tokens.size() - 1)); }
    private Token advance() { return tokens.get(pos++); }
    private static Loc loc(Token t) { return new Loc(t.line(), t.column()); }

    private boolean isPunct(String p) { return peek() instanceof Token.Punct(String text, int l, int c) && text.equals(p); }
    private boolean isKeyword(Token t, String k) { return t instanceof Token.Name(String v, int l, int c) && v.equals(k); }
    private boolean isKeyword(String k) { return isKeyword(peek(), k); }

    private boolean skip(String punct) {
        if (!isPunct(punct)) return false;
        pos++;
        return true;
    }

    private Token expect(String punct) {
        if (!isPunct(punct)) throw unexpected("\"" + punct + "\"");
        return advance();
    }

    private void expectKeyword(String k) {
        if (!isKeyword(k)) throw unexpected("\"" + k + "\"");
        pos++;
    }

    private String name() {
        if (peek() instanceof Token.Name(String v, int l, int c)) { pos++; return v; }
        throw unexpected("Name");
    }

    private SyntaxError unexpected(String expected) {
        Token t = peek();
        return new SyntaxError("Expected " + expected + ", found " + describe(t) + ".", t.line(), t.column());
    }

    private static String describe(Token t) {
        return switch (t) {
            case Token.Punct p -> "\"" + p.text() + "\"";
            case Token.Name n -> "Name \"" + n.value() + "\"";
            case Token.IntValue i -> "Int \"" + i.text() + "\"";
            case Token.FloatValue f -> "Float \"" + f.text() + "\"";
            case Token.StringValue s -> s.block() ? "BlockString" : "String";
            case Token.End e -> "<EOF>";
        };
    }

    /** open item* close, with at least one item. */
    private <T> List<T> many(String open, Supplier<T> item, String close) {
        expect(open);
        List<T> items = new ArrayList<>();
        do { items.add(item.get()); } while (!skip(close));
        return items;
    }

    /** (open item* close)? — an absent list is empty; a present one may not be. */
    private <T> List<T> optionalMany(String open, Supplier<T> item, String close) {
        return isPunct(open) ? many(open, item, close) : List.of();
    }

    // ---- definitions

    private Definition definition() {
        Token first = peek();
        Token keyword = first instanceof Token.StringValue ? peek(1) : first;
        if (first instanceof Token.Punct p && p.text().equals("{")) return operation();
        if (keyword instanceof Token.Name(String k, int l, int c)) {
            switch (k) {
                case "query", "mutation", "subscription" -> { return operation(); }
                case "fragment" -> { return fragment(); }
                case "schema", "scalar", "type", "interface", "union", "enum", "input", "directive" -> { return typeSystem(); }
                case "extend" -> {
                    if (keyword == first) {
                        pos++;
                        Token at = peek();
                        Definition added = typeSystemBody(null, true);
                        if (!addsSomething(added)) throw new SyntaxError("An extension must add something: directives, fields, values, members or interfaces.", at.line(), at.column());
                        return new Extension(added);
                    }
                }
                default -> { }
            }
        }
        throw unexpected(first instanceof Token.StringValue ? "a definition after the description" : "a definition");
    }

    private String description() {
        return peek() instanceof Token.StringValue s ? ((Token.StringValue) advance()).value() : null;
    }

    private Operation operation() {
        Token start = peek();
        String description = description();
        if (description == null && isPunct("{")) return new Operation(loc(start), null, "query", null, List.of(), List.of(), selectionSet());
        String type = name();
        if (!type.equals("query") && !type.equals("mutation") && !type.equals("subscription")) { pos--; throw unexpected("an operation type"); }
        String name = peek() instanceof Token.Name ? name() : null;
        List<VariableDef> variables = optionalMany("(", this::variableDef, ")");
        List<Directive> directives = directives(false);
        return new Operation(loc(start), description, type, name, variables, directives, selectionSet());
    }

    private VariableDef variableDef() {
        Token start = peek();
        String description = description();
        Token dollar = expect("$");
        String name = name();
        expect(":");
        Type type = type();
        Value defaultValue = skip("=") ? value(true) : null;
        return new VariableDef(description == null ? loc(dollar) : loc(start), description, name, type, defaultValue, directives(true));
    }

    private Fragment fragment() {
        Token start = peek();
        String description = description();
        expectKeyword("fragment");
        if (isKeyword("on")) throw unexpected("a fragment name (not \"on\")");
        String name = name();
        expectKeyword("on");
        String typeCondition = name();
        return new Fragment(loc(start), description, name, typeCondition, directives(false), selectionSet());
    }

    // ---- selections

    private List<Selection> selectionSet() { return many("{", this::selection, "}"); }

    private Selection selection() {
        Token start = peek();
        if (skip("...")) {
            if (peek() instanceof Token.Name n && !n.value().equals("on")) return new Spread(loc(start), name(), directives(false));
            String typeCondition = null;
            if (isKeyword("on")) { pos++; typeCondition = name(); }
            return new Inline(loc(start), typeCondition, directives(false), selectionSet());
        }
        String alias = null, name = name();
        if (skip(":")) { alias = name; name = name(); }
        List<Argument> arguments = arguments(false);
        List<Directive> directives = directives(false);
        List<Selection> selections = isPunct("{") ? selectionSet() : List.of();
        return new Field(loc(start), alias, name, arguments, directives, selections);
    }

    private List<Argument> arguments(boolean constant) {
        return optionalMany("(", () -> {
            Token start = peek();
            String name = name();
            expect(":");
            return new Argument(loc(start), name, value(constant));
        }, ")");
    }

    private List<Directive> directives(boolean constant) {
        List<Directive> directives = new ArrayList<>();
        while (isPunct("@")) {
            Token at = advance();
            directives.add(new Directive(loc(at), name(), arguments(constant)));
        }
        return directives;
    }

    // ---- values and types

    private Value value(boolean constant) {
        Token t = peek();
        switch (t) {
            case Token.Punct p when p.text().equals("$") && !constant -> { pos++; return new Variable(loc(t), name()); }
            case Token.Punct p when p.text().equals("[") -> {
                pos++;
                List<Value> values = new ArrayList<>();
                while (!skip("]")) values.add(value(constant));
                return new ListV(values);
            }
            case Token.Punct p when p.text().equals("{") -> {
                pos++;
                List<ObjectField> fields = new ArrayList<>();
                while (!skip("}")) {
                    String name = name();
                    expect(":");
                    fields.add(new ObjectField(name, value(constant)));
                }
                return new ObjectV(fields);
            }
            case Token.IntValue i -> { pos++; return new IntV(i.text()); }
            case Token.FloatValue f -> { pos++; return new FloatV(f.text()); }
            case Token.StringValue s -> { pos++; return new StringV(s.value()); }
            case Token.Name n -> {
                pos++;
                return switch (n.value()) {
                    case "true" -> new BooleanV(true);
                    case "false" -> new BooleanV(false);
                    case "null" -> new NullV();
                    default -> new EnumV(n.value());
                };
            }
            default -> throw unexpected(constant ? "a constant value" : "a value");
        }
    }

    private Type type() {
        Type type;
        if (skip("[")) {
            type = new ListType(type());
            expect("]");
        } else type = new NamedType(name());
        return skip("!") ? new NonNullType(type) : type;
    }

    // ---- type system (spec 3)

    private Definition typeSystem() { return typeSystemBody(description(), false); }

    private Definition typeSystemBody(String description, boolean extension) {
        String keyword = name();
        return switch (keyword) {
            case "schema" -> {
                List<Directive> directives = directives(true);
                yield new SchemaDef(description, directives, extension && !isPunct("{") ? List.of() : many("{", () -> {
                    String op = name();
                    expect(":");
                    return new RootOperation(op, name());
                }, "}"));
            }
            case "scalar" -> new ScalarDef(description, name(), directives(true));
            case "type", "interface" -> {
                String name = name();
                List<String> interfaces = new ArrayList<>();
                if (isKeyword("implements")) {
                    pos++;
                    skip("&");
                    do { interfaces.add(name()); } while (skip("&"));
                }
                List<Directive> directives = directives(true);
                List<FieldDef> fields = optionalMany("{", this::fieldDef, "}");
                yield keyword.equals("type") ? new ObjectDef(description, name, interfaces, directives, fields)
                        : new InterfaceDef(description, name, interfaces, directives, fields);
            }
            case "union" -> {
                String name = name();
                List<Directive> directives = directives(true);
                List<String> members = new ArrayList<>();
                if (skip("=")) {
                    skip("|");
                    do { members.add(name()); } while (skip("|"));
                }
                yield new UnionDef(description, name, directives, members);
            }
            case "enum" -> {
                String name = name();
                List<Directive> directives = directives(true);
                yield new EnumDef(description, name, directives, optionalMany("{", () -> {
                    String d = description();
                    if (isKeyword("true") || isKeyword("false") || isKeyword("null")) throw unexpected("an enum value (not true, false or null)");
                    return new EnumValueDef(d, name(), directives(true));
                }, "}"));
            }
            case "input" -> {
                String name = name();
                List<Directive> directives = directives(true);
                yield new InputDef(description, name, directives, optionalMany("{", this::inputValueDef, "}"));
            }
            case "directive" -> {
                if (extension) { pos--; throw unexpected("a type system extension"); }
                expect("@");
                String name = name();
                List<InputValueDef> arguments = optionalMany("(", this::inputValueDef, ")");
                boolean repeatable = isKeyword("repeatable");
                if (repeatable) pos++;
                expectKeyword("on");
                skip("|");
                List<String> locations = new ArrayList<>();
                do { locations.add(name()); } while (skip("|"));
                yield new DirectiveDef(description, name, arguments, repeatable, locations);
            }
            default -> { pos--; throw unexpected("a type system definition"); }
        };
    }

    /** Every extension production requires at least one of its optional parts (spec 3.2.2, 3.4.3, ...). */
    private static boolean addsSomething(Definition d) {
        return switch (d) {
            case SchemaDef x -> !x.directives().isEmpty() || !x.operations().isEmpty();
            case ScalarDef x -> !x.directives().isEmpty();
            case ObjectDef x -> !x.interfaces().isEmpty() || !x.directives().isEmpty() || !x.fields().isEmpty();
            case InterfaceDef x -> !x.interfaces().isEmpty() || !x.directives().isEmpty() || !x.fields().isEmpty();
            case UnionDef x -> !x.directives().isEmpty() || !x.members().isEmpty();
            case EnumDef x -> !x.directives().isEmpty() || !x.values().isEmpty();
            case InputDef x -> !x.directives().isEmpty() || !x.fields().isEmpty();
            default -> false;
        };
    }

    private FieldDef fieldDef() {
        String description = description();
        String name = name();
        List<InputValueDef> arguments = optionalMany("(", this::inputValueDef, ")");
        expect(":");
        return new FieldDef(description, name, arguments, type(), directives(true));
    }

    private InputValueDef inputValueDef() {
        String description = description();
        String name = name();
        expect(":");
        Type type = type();
        Value defaultValue = skip("=") ? value(true) : null;
        return new InputValueDef(description, name, type, defaultValue, directives(true));
    }
}
