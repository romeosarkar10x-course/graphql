package dev.shelfie.mini;

import dev.shelfie.mini.Ast.Type;
import java.util.Map;
import java.util.Set;

/**
 * Lab 17.6: introspection (spec 4) as resolvers over the schema. The introspection types are already in every
 * schema (Schema.BUILT_INS). The executor calls resolve for __schema and __type on the query type and for every
 * field of an introspection type. Represent a __Type as a TypeRef, a __Field as a FieldDef, an __InputValue as an
 * InputValueDef, an __EnumValue as an EnumValueDef and a __Directive as a DirectiveDef.
 */
public final class Introspection {
    public static final Set<String> BUILT_IN_DIRECTIVES = Set.of("include", "skip", "deprecated", "specifiedBy", "oneOf");

    /** A type reference: a named type, or a list or non-null wrapper around one. */
    public record TypeRef(Type type) {}

    private Introspection() {}

    static Object resolve(Schema schema, String parent, String field, Object source, Map<String, Object> args) {
        throw new UnsupportedOperationException("TODO lab 17.6: introspection, " + parent + "." + field);
    }
}
