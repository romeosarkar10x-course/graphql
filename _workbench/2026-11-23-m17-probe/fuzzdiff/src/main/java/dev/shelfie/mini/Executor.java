package dev.shelfie.mini;

import dev.shelfie.mini.Ast.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Execution (spec 6), one method per algorithm: GetOperation, CoerceVariableValues, ExecuteSelectionSet,
 * CollectFields, ExecuteField, CoerceArgumentValues, CompleteValue, ResolveAbstractType. Query fields run in
 * parallel, one virtual thread each; mutation root fields run one after another (6.3.1).
 */
public final class Executor {
    /** A request error (6.1, 6.2): the response has errors and no data. */
    public static final class RequestError extends RuntimeException {
        public RequestError(String message) { super(message); }
    }

    /** A null travelling up to the nearest nullable position; its error is already recorded (6.4.4). */
    private static final class Bubble extends RuntimeException {
        Bubble() { super(null, null, false, false); }
    }

    private final Schema schema;
    private final Map<String, Fragment> fragments = new LinkedHashMap<>();
    private final Resolver resolver;
    private final List<Map<String, Object>> errors = Collections.synchronizedList(new ArrayList<>());
    private Map<String, Object> variables;
    private ExecutorService threads;

    private Executor(Schema schema, Document doc, Resolver resolver) {
        this.schema = schema;
        this.resolver = resolver;
        for (Definition d : doc.definitions()) if (d instanceof Fragment f) fragments.putIfAbsent(f.name(), f);
    }

    /** ExecuteRequest(): the response as a map with "data" and, when there are any, "errors". */
    public static Map<String, Object> execute(Schema schema, Document doc, String operationName, Map<String, Object> variables,
                                              Resolver resolver, Object root) {
        Executor x = new Executor(schema, doc, resolver);
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            Operation op = operation(doc, operationName);
            x.variables = x.coerceVariables(op, variables == null ? Map.of() : variables);
            String type = schema.root(op.type());
            if (type == null) throw new RequestError("Schema is not configured for " + op.type() + "s.");
            if (op.type().equals("subscription")) throw new RequestError("Subscriptions are not supported.");
            Object data;
            try (ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
                x.threads = threads;
                data = x.selectionSet(type, root, op.selections(), List.of(), op.type().equals("mutation"));
            } catch (Bubble b) {
                data = null;
            }
            if (!x.errors.isEmpty()) response.put("errors", new ArrayList<>(x.errors));
            response.put("data", data);
        } catch (RequestError e) {
            response.put("errors", List.of(Map.of("message", e.getMessage())));
        }
        return response;
    }

    /** GetOperation(document, operationName), 6.1. */
    static Operation operation(Document doc, String name) {
        List<Operation> ops = doc.definitions().stream().filter(d -> d instanceof Operation).map(d -> (Operation) d).toList();
        if (name == null) {
            if (ops.size() == 1) return ops.getFirst();
            throw new RequestError(ops.isEmpty() ? "No operation in the document." : "Must provide operation name if query contains multiple operations.");
        }
        return ops.stream().filter(o -> name.equals(o.name())).findFirst().orElseThrow(() -> new RequestError("Unknown operation named \"" + name + "\"."));
    }

    // ---- 6.1.2 CoerceVariableValues and the input coercion rules of 3.5 and 3.10

    private Map<String, Object> coerceVariables(Operation op, Map<String, Object> given) {
        Map<String, Object> coerced = new LinkedHashMap<>();
        for (VariableDef v : op.variables()) {
            String what = "Variable \"$" + v.name() + "\"";
            if (!given.containsKey(v.name())) {
                if (v.defaultValue() != null) coerced.put(v.name(), literal(v.defaultValue(), v.type(), Map.of(), what));
                else if (v.type() instanceof NonNullType) throw new RequestError(what + " of required type \"" + Schema.print(v.type()) + "\" was not provided.");
            } else {
                try {
                    coerced.put(v.name(), input(given.get(v.name()), v.type(), what));
                } catch (IllegalArgumentException e) {
                    throw new RequestError(e.getMessage());
                }
            }
        }
        return coerced;
    }

    /** Coerces a value from the request's variables (JSON) to the input type. */
    private Object input(Object value, Type type, String what) {
        if (type instanceof NonNullType nn) {
            if (value == null) throw new IllegalArgumentException(what + " of non-null type \"" + Schema.print(type) + "\" must not be null.");
            return input(value, nn.of(), what);
        }
        if (value == null) return null;
        if (type instanceof ListType l) {
            if (value instanceof List<?> list) {
                List<Object> out = new ArrayList<>();
                for (Object item : list) out.add(input(item, l.of(), what));
                return out;
            }
            return singleton(input(value, l.of(), what));   // list input coercion: one value becomes a list of one
        }
        String name = Schema.named(type);
        return switch (schema.types.get(name)) {
            case InputDef def -> {
                if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException(what + " expected an object for \"" + name + "\".");
                for (Object key : map.keySet())
                    if (def.fields().stream().noneMatch(f -> f.name().equals(key))) throw new IllegalArgumentException(what + ": \"" + key + "\" is not a field of \"" + name + "\".");
                Map<String, Object> out = new LinkedHashMap<>();
                for (InputValueDef f : def.fields()) {
                    if (map.containsKey(f.name())) out.put(f.name(), input(map.get(f.name()), f.type(), what));
                    else if (f.defaultValue() != null) out.put(f.name(), literal(f.defaultValue(), f.type(), Map.of(), what));
                    else if (f.type() instanceof NonNullType) throw new IllegalArgumentException(what + ": field \"" + name + "." + f.name() + "\" of required type \"" + Schema.print(f.type()) + "\" was not provided.");
                }
                oneOf(def, out, what);
                yield out;
            }
            case EnumDef def -> {
                if (value instanceof String s && def.values().stream().anyMatch(v -> v.name().equals(s))) yield s;
                throw new IllegalArgumentException(what + ": \"" + value + "\" is not a value of enum \"" + name + "\".");
            }
            case ScalarDef s -> scalarInput(name, value, what);
            case null, default -> throw new IllegalArgumentException(what + ": \"" + name + "\" is not an input type.");
        };
    }

    private static List<Object> singleton(Object item) {
        List<Object> one = new ArrayList<>();
        one.add(item);
        return one;
    }

    private static void oneOf(InputDef def, Map<String, Object> value, String what) {
        if (def.directives().stream().noneMatch(d -> d.name().equals("oneOf"))) return;
        if (value.size() != 1 || value.values().iterator().next() == null)
            throw new IllegalArgumentException(what + ": OneOf input object \"" + def.name() + "\" must have exactly one non-null field.");
    }

    private static Object scalarInput(String name, Object v, String what) {
        Object out = switch (name) {
            case "Int" -> v instanceof Integer || v instanceof Long l && l == l.intValue() ? ((Number) v).intValue() : null;
            case "Float" -> v instanceof Number n && !(v instanceof Double d && d.isInfinite()) ? n.doubleValue() : null;
            case "String" -> v instanceof String ? v : null;
            case "Boolean" -> v instanceof Boolean ? v : null;
            case "ID" -> v instanceof String || v instanceof Integer || v instanceof Long ? v.toString() : null;
            default -> v;   // custom scalars pass through
        };
        if (out == null) throw new IllegalArgumentException(what + ": " + name + " cannot represent " + v + ".");
        return out;
    }

    /** Coerces a literal from the document (3.5, 3.9, 3.10 "Input Coercion"), resolving variables inside it. */
    private Object literal(Value value, Type type, Map<String, Object> vars, String what) {
        if (value instanceof Variable v) {
            if (vars.containsKey(v.name())) return vars.get(v.name());
            return Absent.VALUE;
        }
        if (type instanceof NonNullType nn) {
            if (value instanceof NullV) throw new IllegalArgumentException(what + ": null for non-null type \"" + Schema.print(type) + "\".");
            Object out = literal(value, nn.of(), vars, what);
            if (out == Absent.VALUE) throw new IllegalArgumentException(what + ": required type \"" + Schema.print(type) + "\" was not provided a value.");
            return out;
        }
        if (value instanceof NullV) return null;
        if (type instanceof ListType l) {
            if (value instanceof ListV list) {
                List<Object> out = new ArrayList<>();
                for (Value item : list.values()) {
                    Object o = literal(item, l.of(), vars, what);
                    out.add(o == Absent.VALUE ? null : o);
                }
                return out;
            }
            Object one = literal(value, l.of(), vars, what);
            return one == Absent.VALUE ? one : singleton(one);
        }
        String name = Schema.named(type);
        return switch (schema.types.get(name)) {
            case InputDef def -> {
                if (!(value instanceof ObjectV obj)) throw new IllegalArgumentException(what + ": expected an object for \"" + name + "\".");
                Map<String, Object> out = new LinkedHashMap<>();
                for (InputValueDef f : def.fields()) {
                    ObjectField given = obj.fields().stream().filter(x -> x.name().equals(f.name())).findFirst().orElse(null);
                    Object v = given == null ? Absent.VALUE : literal(given.value(), f.type(), vars, what);
                    if (v != Absent.VALUE) out.put(f.name(), v);
                    else if (f.defaultValue() != null) out.put(f.name(), literal(f.defaultValue(), f.type(), Map.of(), what));
                    else if (f.type() instanceof NonNullType) throw new IllegalArgumentException(what + ": field \"" + name + "." + f.name() + "\" of required type \"" + Schema.print(f.type()) + "\" was not provided.");
                }
                oneOf(def, out, what);
                yield out;
            }
            case EnumDef def -> {
                if (value instanceof EnumV e && def.values().stream().anyMatch(v -> v.name().equals(e.name()))) yield e.name();
                throw new IllegalArgumentException(what + ": " + Printer.value(value) + " is not a value of enum \"" + name + "\".");
            }
            case ScalarDef s -> {
                Object out = switch (value) {
                    case IntV i when name.equals("Int") -> { long l = Long.parseLong(i.text()); yield l == (int) l ? (Object) (int) l : null; }
                    case IntV i when name.equals("Float") -> Double.parseDouble(i.text());
                    case FloatV f when name.equals("Float") -> Double.parseDouble(f.text());
                    case StringV str when name.equals("String") || name.equals("ID") -> str.value();
                    case IntV i when name.equals("ID") -> i.text();
                    case BooleanV b when name.equals("Boolean") -> b.value();
                    default -> SCALARS_BUILT_IN.contains(name) ? null : Printer.value(value);
                };
                if (out == null) throw new IllegalArgumentException(what + ": " + name + " cannot represent " + Printer.value(value) + ".");
                yield out;
            }
            case null, default -> throw new IllegalArgumentException(what + ": \"" + name + "\" is not an input type.");
        };
    }

    private static final Set<String> SCALARS_BUILT_IN = Set.copyOf(Schema.SCALARS);

    /** An argument or input field that was given a variable the request did not provide. */
    private enum Absent { VALUE }

    // ---- 6.3 Executing Selection Sets

    private Map<String, Object> selectionSet(String type, Object value, List<Selection> selections, List<Object> path, boolean serial) {
        Map<String, List<Field>> grouped = collectFields(type, selections, new HashSet<>(), new LinkedHashMap<>());
        Map<String, Object> result = new LinkedHashMap<>();
        if (serial) {
            for (var e : grouped.entrySet()) result.put(e.getKey(), field(type, value, e.getValue(), append(path, e.getKey())));
            return result;
        }
        Map<String, Future<Object>> running = new LinkedHashMap<>();
        for (var e : grouped.entrySet())
            running.put(e.getKey(), threads.submit(() -> field(type, value, e.getValue(), append(path, e.getKey()))));
        for (var e : running.entrySet()) {
            try {
                result.put(e.getKey(), e.getValue().get());
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof RuntimeException r) throw r;
                throw new IllegalStateException(ex.getCause());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }
        return result;
    }

    /** CollectFields(objectType, selectionSet, variableValues, visitedFragments), 6.3.2. */
    private Map<String, List<Field>> collectFields(String type, List<Selection> selections, Set<String> visited, Map<String, List<Field>> out) {
        for (Selection s : selections) {
            switch (s) {
                case Field f -> { if (included(f.directives())) out.computeIfAbsent(f.responseKey(), k -> new ArrayList<>()).add(f); }
                case Spread sp -> {
                    Fragment f = fragments.get(sp.name());
                    if (!included(sp.directives()) || f == null || !visited.add(sp.name()) || !applies(type, f.typeCondition())) continue;
                    collectFields(type, f.selections(), visited, out);
                }
                case Inline i -> {
                    if (included(i.directives()) && (i.typeCondition() == null || applies(type, i.typeCondition())))
                        collectFields(type, i.selections(), visited, out);
                }
            }
        }
        return out;
    }

    /** @skip and @include (3.13.2, 3.13.3). */
    private boolean included(List<Directive> directives) {
        for (Directive d : directives) {
            if (!d.name().equals("skip") && !d.name().equals("include")) continue;
            Argument arg = d.arguments().stream().filter(a -> a.name().equals("if")).findFirst().orElse(null);
            Object v = arg == null ? null : literal(arg.value(), new NonNullType(new NamedType("Boolean")), variables, "@" + d.name());
            if (d.name().equals("skip") && Boolean.TRUE.equals(v) || d.name().equals("include") && !Boolean.TRUE.equals(v)) return false;
        }
        return true;
    }

    /** DoesFragmentTypeApply(objectType, fragmentType). */
    private boolean applies(String objectType, String fragmentType) {
        return objectType.equals(fragmentType) || schema.possibleTypes(fragmentType).contains(objectType) && schema.isAbstract(fragmentType);
    }

    // ---- 6.4 Executing Fields

    /** ExecuteField(objectType, objectValue, fieldType, fields, variableValues). */
    private Object field(String type, Object value, List<Field> fields, List<Object> path) {
        Field f = fields.getFirst();
        FieldDef def = schema.field(type, f.name());
        if (def == null) { error("Cannot query field \"" + f.name() + "\" on type \"" + type + "\".", fields, path); return null; }
        return guarded(def.type(), fields, path, () -> {
            Map<String, Object> args = coerceArguments(type, def, f);
            Object raw;
            if (f.name().equals("__typename")) raw = type;
            else if (f.name().equals("__schema") || f.name().equals("__type") || type.startsWith("__")) raw = Introspection.resolve(schema, type, f.name(), value, args);
            else raw = resolver.resolve(type, f.name(), value, args);
            return complete(def.type(), fields, raw, path);
        });
    }

    /** Runs work for one position; an error makes the position null, or the parent when the position is non-null. */
    private Object guarded(Type type, List<Field> fields, List<Object> path, Callable<Object> work) {
        try {
            return work.call();
        } catch (Bubble b) {
            if (type instanceof NonNullType) throw b;
            return null;
        } catch (Exception e) {
            error(e.getMessage() == null ? e.toString() : e.getMessage(), fields, path);
            if (type instanceof NonNullType) throw new Bubble();
            return null;
        }
    }

    private void error(String message, List<Field> fields, List<Object> path) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("message", message);
        Loc l = fields.getFirst().loc();
        e.put("locations", List.of(Map.of("line", l.line(), "column", l.column())));
        e.put("path", path);
        errors.add(e);
    }

    /** CoerceArgumentValues(objectType, field, variableValues), 6.4.1. */
    private Map<String, Object> coerceArguments(String type, FieldDef def, Field f) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (InputValueDef a : def.arguments()) {
            Argument given = f.arguments().stream().filter(x -> x.name().equals(a.name())).findFirst().orElse(null);
            String what = "Argument \"" + a.name() + "\" of \"" + type + "." + def.name() + "\"";
            Object v = given == null ? Absent.VALUE : literal(given.value(), a.type(), variables, what);
            if (v != Absent.VALUE) out.put(a.name(), v);
            else if (a.defaultValue() != null) out.put(a.name(), literal(a.defaultValue(), a.type(), Map.of(), what));
            else if (a.type() instanceof NonNullType) throw new IllegalArgumentException(what + " of required type \"" + Schema.print(a.type()) + "\" was not provided.");
        }
        return out;
    }

    /** CompleteValue(fieldType, fields, result, variableValues), 6.4.3. */
    private Object complete(Type type, List<Field> fields, Object result, List<Object> path) throws Exception {
        if (type instanceof NonNullType nn) {
            Object v = complete(nn.of(), fields, result, path);
            if (v == null) {
                Field f = fields.getFirst();
                error("Cannot return null for non-nullable field " + Schema.print(type) + " at " + f.name() + ".", fields, path);
                throw new Bubble();
            }
            return v;
        }
        if (result == null) return null;
        if (type instanceof ListType l) {
            Iterable<?> items = result instanceof Iterable<?> it ? it : result instanceof Object[] arr ? List.of(arr) : null;
            if (items == null) throw new IllegalStateException("Expected a list for a field of type \"" + Schema.print(type) + "\".");
            List<Object> out = new ArrayList<>();
            int i = 0;
            for (Object item : items) {
                List<Object> itemPath = append(path, i++);
                out.add(guarded(l.of(), fields, itemPath, () -> complete(l.of(), fields, item, itemPath)));
            }
            return out;
        }
        String name = Schema.named(type);
        return switch (schema.types.get(name)) {
            case ScalarDef s -> serialize(name, result);
            case EnumDef e -> {
                String v = result instanceof Enum<?> en ? en.name() : result.toString();
                if (e.values().stream().noneMatch(x -> x.name().equals(v))) throw new IllegalStateException("Enum \"" + name + "\" cannot represent value: " + v);
                yield v;
            }
            default -> {
                String objectType = schema.isAbstract(name) ? resolver.typeOf(result) : name;   // ResolveAbstractType
                if (!schema.possibleTypes(name).contains(objectType))
                    throw new IllegalStateException("Abstract type \"" + name + "\" resolved to \"" + objectType + "\", which is not one of its possible types.");
                List<Selection> sub = new ArrayList<>();
                for (Field f : fields) sub.addAll(f.selections());
                yield selectionSet(objectType, result, sub, path, false);
            }
        };
    }

    /** Result coercion of the built-in scalars (3.5); custom scalars are returned as the resolver gave them. */
    private static Object serialize(String scalar, Object v) {
        Object out = switch (scalar) {
            case "Int" -> v instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue()) && n.longValue() == n.intValue() ? (Object) n.intValue() : null;
            case "Float" -> v instanceof Number n && Double.isFinite(n.doubleValue()) ? (Object) n.doubleValue() : null;
            case "String" -> v instanceof String || v instanceof Character || v instanceof Number || v instanceof Boolean ? v.toString() : null;
            case "Boolean" -> v instanceof Boolean ? v : null;
            case "ID" -> v instanceof String || v instanceof Integer || v instanceof Long ? v.toString() : null;
            default -> v;
        };
        if (out == null) throw new IllegalStateException(scalar + " cannot represent value: " + v);
        return out;
    }

    private static List<Object> append(List<Object> path, Object key) {
        List<Object> p = new ArrayList<>(path);
        p.add(key);
        return Collections.unmodifiableList(p);
    }
}
