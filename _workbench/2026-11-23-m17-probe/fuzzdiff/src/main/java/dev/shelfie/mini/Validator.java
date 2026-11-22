package dev.shelfie.mini;

import dev.shelfie.mini.Ast.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Six of the spec's validation rules (5.3.1, 5.3.2, 5.4.1, 5.5.2.2, 5.8.4, 5.8.5). The other rules are not
 * implemented: the executor assumes documents that pass them, and reports what it cannot execute as an error.
 */
public final class Validator {
    public record Problem(String rule, String message, List<Loc> locations) {}

    private final Schema schema;
    private final Map<String, Fragment> fragments = new LinkedHashMap<>();
    private final List<Problem> problems = new ArrayList<>();

    private Validator(Schema schema, Document doc) {
        this.schema = schema;
        for (Definition d : doc.definitions()) if (d instanceof Fragment f) fragments.putIfAbsent(f.name(), f);
    }

    public static List<Problem> validate(Schema schema, Document doc) {
        Validator v = new Validator(schema, doc);
        for (Definition d : doc.definitions()) {
            switch (d) {
                case Operation op -> {
                    v.directives(op.directives());
                    op.variables().forEach(var -> v.directives(var.directives()));
                    v.fields(schema.root(op.type()), op.selections());
                    v.variables(op);
                    if (schema.root(op.type()) != null) v.overlaps(schema.root(op.type()), op.selections());
                }
                case Fragment f -> {
                    v.directives(f.directives());
                    String type = schema.isComposite(f.typeCondition()) ? f.typeCondition() : null;
                    v.fields(type, f.selections());
                    if (type != null) v.overlaps(type, f.selections());
                }
                default -> { }
            }
        }
        v.cycles();
        return v.problems;
    }

    private void problem(String rule, String message, Loc... locations) { problems.add(new Problem(rule, message, List.of(locations))); }

    private String composite(Type t) { return t != null && schema.isComposite(Schema.named(t)) ? Schema.named(t) : null; }

    // ---- 5.3.1 Field Selections on Objects, Interfaces and Unions Types; 5.4.1 Argument Names

    private void fields(String parent, List<Selection> selections) {
        for (Selection s : selections) {
            switch (s) {
                case Field f -> {
                    FieldDef def = parent == null ? null : schema.field(parent, f.name());
                    if (parent != null && def == null)
                        problem("FieldsOnCorrectType", "Cannot query field \"" + f.name() + "\" on type \"" + parent + "\".", f.loc());
                    if (def != null)
                        for (Argument a : f.arguments())
                            if (def.arguments().stream().noneMatch(ad -> ad.name().equals(a.name())))
                                problem("KnownArgumentNames", "Unknown argument \"" + a.name() + "\" on field \"" + parent + "." + f.name() + "\".", a.loc());
                    directives(f.directives());
                    fields(def == null ? null : composite(def.type()), f.selections());
                }
                case Inline i -> {
                    directives(i.directives());
                    fields(i.typeCondition() == null ? parent : schema.isComposite(i.typeCondition()) ? i.typeCondition() : null, i.selections());
                }
                case Spread sp -> directives(sp.directives());
            }
        }
    }

    private void directives(List<Directive> directives) {
        for (Directive d : directives) {
            DirectiveDef def = schema.directives.get(d.name());
            if (def != null)
                for (Argument a : d.arguments())
                    if (def.arguments().stream().noneMatch(ad -> ad.name().equals(a.name())))
                        problem("KnownArgumentNames", "Unknown argument \"" + a.name() + "\" on directive \"@" + d.name() + "\".", a.loc());
        }
    }

    // ---- 5.8.4 All Variables Used; 5.8.5 All Variable Usages Are Allowed

    /** A variable where a value is expected: the location's type (null when unknown) and whether it has a default. */
    record Usage(Variable variable, Type type, boolean locationHasDefault) {}

    private void variables(Operation op) {
        List<Usage> usages = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        directiveUsages(op.directives(), usages);
        usages(schema.root(op.type()), op.selections(), usages, seen);
        String in = op.name() == null ? "." : " in operation \"" + op.name() + "\".";
        for (VariableDef v : op.variables()) {
            if (usages.stream().noneMatch(u -> u.variable().name().equals(v.name())))
                problem("NoUnusedVariables", "Variable \"$" + v.name() + "\" is never used" + in, v.loc());
        }
        for (Usage u : usages) {
            VariableDef def = op.variables().stream().filter(v -> v.name().equals(u.variable().name())).findFirst().orElse(null);
            if (def == null || u.type() == null || !schema.isInput(Schema.named(def.type()))) continue;
            if (!allowed(def, u))
                problem("VariablesInAllowedPosition", "Variable \"$" + def.name() + "\" of type \"" + Schema.print(def.type())
                        + "\" used in position expecting type \"" + Schema.print(u.type()) + "\".", def.loc(), u.variable().loc());
        }
    }

    /** IsVariableUsageAllowed(variableDefinition, variableUsage), spec 5.8.5. */
    private static boolean allowed(VariableDef def, Usage u) {
        Type varType = def.type(), locType = u.type();
        if (locType instanceof NonNullType nn && !(varType instanceof NonNullType)) {
            boolean varDefault = def.defaultValue() != null && !(def.defaultValue() instanceof NullV);
            if (!varDefault && !u.locationHasDefault()) return false;
            return compatible(varType, nn.of());
        }
        return compatible(varType, locType);
    }

    /** AreTypesCompatible(variableType, locationType). */
    private static boolean compatible(Type var, Type loc) {
        if (loc instanceof NonNullType l) return var instanceof NonNullType v && compatible(v.of(), l.of());
        if (var instanceof NonNullType v) return compatible(v.of(), loc);
        if (loc instanceof ListType l) return var instanceof ListType v && compatible(v.of(), l.of());
        if (var instanceof ListType) return false;
        return Schema.named(var).equals(Schema.named(loc));
    }

    private void usages(String parent, List<Selection> selections, List<Usage> out, Set<String> seenFragments) {
        for (Selection s : selections) {
            switch (s) {
                case Field f -> {
                    FieldDef def = parent == null ? null : schema.field(parent, f.name());
                    for (Argument a : f.arguments()) {
                        InputValueDef ad = def == null ? null : def.arguments().stream().filter(x -> x.name().equals(a.name())).findFirst().orElse(null);
                        valueUsages(a.value(), ad == null ? null : ad.type(), ad != null && ad.defaultValue() != null, out);
                    }
                    directiveUsages(f.directives(), out);
                    usages(def == null ? null : composite(def.type()), f.selections(), out, seenFragments);
                }
                case Inline i -> {
                    directiveUsages(i.directives(), out);
                    usages(i.typeCondition() == null ? parent : schema.isComposite(i.typeCondition()) ? i.typeCondition() : null, i.selections(), out, seenFragments);
                }
                case Spread sp -> {
                    directiveUsages(sp.directives(), out);
                    Fragment f = fragments.get(sp.name());
                    if (f != null && seenFragments.add(f.name())) {
                        directiveUsages(f.directives(), out);
                        usages(schema.isComposite(f.typeCondition()) ? f.typeCondition() : null, f.selections(), out, seenFragments);
                    }
                }
            }
        }
    }

    private void directiveUsages(List<Directive> directives, List<Usage> out) {
        for (Directive d : directives) {
            DirectiveDef def = schema.directives.get(d.name());
            for (Argument a : d.arguments()) {
                InputValueDef ad = def == null ? null : def.arguments().stream().filter(x -> x.name().equals(a.name())).findFirst().orElse(null);
                valueUsages(a.value(), ad == null ? null : ad.type(), ad != null && ad.defaultValue() != null, out);
            }
        }
    }

    private void valueUsages(Value v, Type type, boolean hasDefault, List<Usage> out) {
        switch (v) {
            case Variable var -> out.add(new Usage(var, type, hasDefault));
            case ListV l -> {
                Type item = type != null && Schema.nullable(type) instanceof ListType lt ? lt.of() : null;
                for (Value x : l.values()) valueUsages(x, item, false, out);
            }
            case ObjectV o -> {
                InputDef input = type != null && schema.types.get(Schema.named(type)) instanceof InputDef i ? i : null;
                for (ObjectField f : o.fields()) {
                    InputValueDef fd = input == null ? null : input.fields().stream().filter(x -> x.name().equals(f.name())).findFirst().orElse(null);
                    valueUsages(f.value(), fd == null ? null : fd.type(), fd != null && fd.defaultValue() != null, out);
                }
            }
            default -> { }
        }
    }

    // ---- 5.5.2.2 Fragment Spreads Must Not Form Cycles: one error per cycle found, depth first

    private void cycles() {
        Set<String> done = new HashSet<>();
        for (String name : fragments.keySet()) cycles(name, new ArrayList<>(), done);
    }

    private void cycles(String name, List<String> path, Set<String> done) {
        if (path.contains(name)) {
            List<String> cycle = path.subList(path.indexOf(name), path.size());
            String via = cycle.size() == 1 ? "." : " via " + String.join(", ", cycle.subList(1, cycle.size()).stream().map(n -> "\"" + n + "\"").toList()) + ".";
            problem("NoFragmentCycles", "Cannot spread fragment \"" + name + "\" within itself" + via, fragments.get(name).loc());
            return;
        }
        if (!done.add(name)) return;
        Fragment f = fragments.get(name);
        if (f == null) return;
        path.add(name);
        for (String spread : spreads(f.selections(), new ArrayList<>())) cycles(spread, path, done);
        path.removeLast();
    }

    private static List<String> spreads(List<Selection> selections, List<String> out) {
        for (Selection s : selections) {
            switch (s) {
                case Field f -> spreads(f.selections(), out);
                case Inline i -> spreads(i.selections(), out);
                case Spread sp -> out.add(sp.name());
            }
        }
        return out;
    }

    // ---- 5.3.2 Field Selection Merging: FieldsInSetCanMerge(set) and SameResponseShape(fieldA, fieldB)

    /** A field in a set, with the type it was selected on. */
    record Selected(Field field, String parent, FieldDef def) {}

    private void overlaps(String parent, List<Selection> selections) {
        List<Selected> set = new ArrayList<>();
        collect(parent, selections, set, new HashSet<>());
        for (int i = 0; i < set.size(); i++)
            for (int j = i + 1; j < set.size(); j++) {
                Selected a = set.get(i), b = set.get(j);
                String key = a.field().responseKey();
                if (!key.equals(b.field().responseKey())) continue;
                String why = conflict(a, b, !Objects.equals(a.parent(), b.parent()) && isObject(a.parent()) && isObject(b.parent()));
                if (why != null) problem("OverlappingFieldsCanBeMerged", "Fields \"" + key + "\" conflict because " + why
                        + ". Use different aliases on the fields to fetch both if this was intentional.", a.field().loc(), b.field().loc());
            }
        for (Selected s : set) if (s.def() != null && composite(s.def().type()) != null) overlaps(composite(s.def().type()), s.field().selections());
    }

    private void collect(String parent, List<Selection> selections, List<Selected> out, Set<String> seenFragments) {
        for (Selection s : selections) {
            switch (s) {
                case Field f -> out.add(new Selected(f, parent, parent == null ? null : schema.field(parent, f.name())));
                case Inline i -> collect(i.typeCondition() == null ? parent : i.typeCondition(), i.selections(), out, seenFragments);
                case Spread sp -> {
                    Fragment f = fragments.get(sp.name());
                    if (f != null && seenFragments.add(f.name())) collect(f.typeCondition(), f.selections(), out, seenFragments);
                }
            }
        }
    }

    private boolean isObject(String type) { return type != null && schema.types.get(type) instanceof ObjectDef; }

    /** Why two same-key fields cannot merge, or null. Fields on two different object types only need the same shape. */
    private String conflict(Selected a, Selected b, boolean exclusiveParents) {
        if (!exclusiveParents) {
            if (!a.field().name().equals(b.field().name()))
                return "\"" + a.field().name() + "\" and \"" + b.field().name() + "\" are different fields";
            if (!sameArguments(a.field().arguments(), b.field().arguments())) return "they have differing arguments";
        }
        if (a.def() != null && b.def() != null) {
            String shape = shape(a.def().type(), b.def().type());
            if (shape != null) return shape;
        }
        List<Selected> merged = new ArrayList<>();
        String pa = a.def() == null ? null : composite(a.def().type()), pb = b.def() == null ? null : composite(b.def().type());
        collect(pa, a.field().selections(), merged, new HashSet<>());
        int split = merged.size();
        collect(pb, b.field().selections(), merged, new HashSet<>());
        for (int i = 0; i < split; i++)
            for (int j = split; j < merged.size(); j++) {
                Selected x = merged.get(i), y = merged.get(j);
                if (!x.field().responseKey().equals(y.field().responseKey())) continue;
                String why = conflict(x, y, exclusiveParents || !Objects.equals(x.parent(), y.parent()) && isObject(x.parent()) && isObject(y.parent()));
                if (why != null) return "subfields \"" + x.field().responseKey() + "\" conflict because " + why;
            }
        return null;
    }

    /** SameResponseShape on the types alone: the same wrapping, and the same leaf type when either is a leaf. */
    private String shape(Type a, Type b) {
        if (a instanceof NonNullType != b instanceof NonNullType || a instanceof ListType && !(b instanceof ListType)
                || b instanceof ListType && !(a instanceof ListType))
            return "they return conflicting types \"" + Schema.print(a) + "\" and \"" + Schema.print(b) + "\"";
        if (a instanceof NonNullType x && b instanceof NonNullType y) return shape(x.of(), y.of());
        if (a instanceof ListType x && b instanceof ListType y) return shape(x.of(), y.of());
        String na = Schema.named(a), nb = Schema.named(b);
        if ((schema.isLeaf(na) || schema.isLeaf(nb)) && !na.equals(nb))
            return "they return conflicting types \"" + na + "\" and \"" + nb + "\"";
        return null;
    }

    private static boolean sameArguments(List<Argument> a, List<Argument> b) {
        if (a.size() != b.size()) return false;
        for (Argument x : a)
            if (b.stream().noneMatch(y -> y.name().equals(x.name()) && Printer.value(y.value()).equals(Printer.value(x.value())))) return false;
        return true;
    }
}
