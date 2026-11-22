package dev.shelfie.mini;

import java.util.List;

/**
 * The abstract syntax tree (spec 2 and 3) as records. Lists are never null (an absent list is empty); absent names,
 * aliases, type conditions, default values and descriptions are null. A field without a selection set has an empty
 * one. Nodes that errors point at carry the location of their first token.
 */
public interface Ast {
    record Loc(int line, int column) {}

    record Document(List<Definition> definitions) {}

    sealed interface Definition {}

    // ---- executable definitions (2.3, 2.9)

    /**
     * type is "query", "mutation" or "subscription"; the query shorthand { ... } is an unnamed "query". Descriptions on
     * operations, fragments and variables are new in the September 2025 edition (RFC #1170).
     */
    record Operation(Loc loc, String description, String type, String name, List<VariableDef> variables, List<Directive> directives,
                     List<Selection> selections) implements Definition {}

    record Fragment(Loc loc, String description, String name, String typeCondition, List<Directive> directives,
                    List<Selection> selections) implements Definition {}

    sealed interface Selection {}

    record Field(Loc loc, String alias, String name, List<Argument> arguments, List<Directive> directives,
                 List<Selection> selections) implements Selection {
        public String responseKey() { return alias != null ? alias : name; }
    }

    record Spread(Loc loc, String name, List<Directive> directives) implements Selection {}

    record Inline(Loc loc, String typeCondition, List<Directive> directives, List<Selection> selections) implements Selection {}

    record Argument(Loc loc, String name, Value value) {}

    record Directive(Loc loc, String name, List<Argument> arguments) {}

    record VariableDef(Loc loc, String description, String name, Type type, Value defaultValue, List<Directive> directives) {}

    // ---- types (2.12) and values (2.10)

    sealed interface Type {}
    record NamedType(String name) implements Type {}
    record ListType(Type of) implements Type {}
    record NonNullType(Type of) implements Type {}

    sealed interface Value {}
    record Variable(Loc loc, String name) implements Value {}
    record IntV(String text) implements Value {}
    record FloatV(String text) implements Value {}
    record StringV(String value) implements Value {}
    record BooleanV(boolean value) implements Value {}
    record NullV() implements Value {}
    record EnumV(String name) implements Value {}
    record ListV(List<Value> values) implements Value {}
    record ObjectV(List<ObjectField> fields) implements Value {}
    record ObjectField(String name, Value value) {}

    // ---- the type system (3)

    record SchemaDef(String description, List<Directive> directives, List<RootOperation> operations) implements Definition {}
    record RootOperation(String operation, String type) {}

    record ScalarDef(String description, String name, List<Directive> directives) implements Definition {}

    record ObjectDef(String description, String name, List<String> interfaces, List<Directive> directives,
                     List<FieldDef> fields) implements Definition {}

    record InterfaceDef(String description, String name, List<String> interfaces, List<Directive> directives,
                        List<FieldDef> fields) implements Definition {}

    record UnionDef(String description, String name, List<Directive> directives, List<String> members) implements Definition {}

    record EnumDef(String description, String name, List<Directive> directives, List<EnumValueDef> values) implements Definition {}

    record InputDef(String description, String name, List<Directive> directives, List<InputValueDef> fields) implements Definition {}

    record DirectiveDef(String description, String name, List<InputValueDef> arguments, boolean repeatable,
                        List<String> locations) implements Definition {}

    /** extend schema / scalar / type / interface / union / enum / input: the definition holds only what is added. */
    record Extension(Definition definition) implements Definition {}

    record FieldDef(String description, String name, List<InputValueDef> arguments, Type type, List<Directive> directives) {}

    record InputValueDef(String description, String name, Type type, Value defaultValue, List<Directive> directives) {}

    record EnumValueDef(String description, String name, List<Directive> directives) {}
}
