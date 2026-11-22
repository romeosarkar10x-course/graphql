package dev.shelfie.harness;

import dev.shelfie.mini.Ast;
import dev.shelfie.mini.Resolver;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.*;
import graphql.parser.Parser;
import graphql.parser.ParserEnvironment;
import graphql.parser.ParserOptions;
import graphql.parser.StringValueParsing;
import graphql.parser.antlr.GraphqlLexer;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.validation.OperationValidationRule;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import graphql.org.antlr.v4.runtime.BaseErrorListener;
import graphql.org.antlr.v4.runtime.CharStreams;
import graphql.org.antlr.v4.runtime.RecognitionException;
import graphql.org.antlr.v4.runtime.Recognizer;

/** graphql-java 26.1 as the course's second opinion: its tokens, its AST as ours, its validation and execution. */
public final class Gj {
    private Gj() {}

    /**
     * graphql-java's tokens as "Kind value @line:column" strings, the way TokenDiff prints yours. Block string values
     * come from the spec's BlockStringValue() applied to graphql-java's raw token (see Known.blockString).
     */
    public static List<String> tokens(String source) {
        GraphqlLexer lexer = new GraphqlLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> r, Object o, int line, int col, String msg, RecognitionException e) {
                throw new IllegalArgumentException("line " + line + ":" + (col + 1) + " " + msg);
            }
        });
        List<String> out = new ArrayList<>();
        for (graphql.org.antlr.v4.runtime.Token t = lexer.nextToken(); t.getType() != graphql.org.antlr.v4.runtime.Token.EOF; t = lexer.nextToken()) {
            if (t.getChannel() != 0) continue;
            String name = GraphqlLexer.VOCABULARY.getSymbolicName(t.getType()), text = t.getText();
            String kind = switch (name == null ? "" : name) {
                case "IntValue" -> "Int";
                case "FloatValue" -> "Float";
                case "StringValue" -> "String";
                case "" -> "Punct";
                default -> "Name";
            };
            String value = switch (kind) {
                case "String" -> text.startsWith("\"\"\"") ? Known.blockString(text)   // the spec's algorithm, see Known
                        : StringValueParsing.parseSingleQuotedString(graphql.i18n.I18n.i18n(graphql.i18n.I18n.BundleType.Parsing, Locale.ENGLISH), text, null);
                default -> text;
            };
            out.add(kind + " " + value + " @" + t.getLine() + ":" + (t.getCharPositionInLine() + 1));
        }
        return out;
    }

    /** The raw text of every block string token graphql-java's lexer finds. */
    public static List<String> blockStringTokens(String source) {
        GraphqlLexer lexer = new GraphqlLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        List<String> out = new ArrayList<>();
        for (graphql.org.antlr.v4.runtime.Token t = lexer.nextToken(); t.getType() != graphql.org.antlr.v4.runtime.Token.EOF; t = lexer.nextToken())
            if (t.getText().startsWith("\"\"\"") && "StringValue".equals(GraphqlLexer.VOCABULARY.getSymbolicName(t.getType()))) out.add(t.getText());
        return out;
    }

    /** graphql-java's parser with no token limit (the corpus has large documents). */
    public static Document parse(String source) {
        ParserOptions options = ParserOptions.newParserOptions().maxTokens(Integer.MAX_VALUE).maxWhitespaceTokens(Integer.MAX_VALUE)
                .maxCharacters(Integer.MAX_VALUE).maxRuleDepth(Integer.MAX_VALUE).build();
        return new Parser().parseDocument(ParserEnvironment.newParserEnvironment().document(source).parserOptions(options).build());
    }

    // ---- graphql-java's AST as our records

    public static Ast.Document toAst(Document d) {
        return new Ast.Document(d.getDefinitions().stream().map(Gj::definition).toList());
    }

    private static Ast.Loc loc(Node<?> n) { return new Ast.Loc(n.getSourceLocation().getLine(), n.getSourceLocation().getColumn()); }
    private static String desc(Description d) { return d == null ? null : d.getContent(); }

    private static Ast.Definition definition(Definition<?> def) {
        return switch (def) {
            case OperationDefinition o -> new Ast.Operation(loc(o), null, o.getOperation().name().toLowerCase(Locale.ROOT), o.getName(),
                    o.getVariableDefinitions().stream().map(v -> new Ast.VariableDef(loc(v), null, v.getName(), type(v.getType()),
                            v.getDefaultValue() == null ? null : value(v.getDefaultValue()), directives(v.getDirectives()))).toList(),
                    directives(o.getDirectives()), selections(o.getSelectionSet()));
            case FragmentDefinition f -> new Ast.Fragment(loc(f), null, f.getName(), f.getTypeCondition().getName(),
                    directives(f.getDirectives()), selections(f.getSelectionSet()));
            case SchemaExtensionDefinition s -> new Ast.Extension(new Ast.SchemaDef(null, directives(s.getDirectives()), roots(s.getOperationTypeDefinitions())));
            case SchemaDefinition s -> new Ast.SchemaDef(desc(s.getDescription()), directives(s.getDirectives()), roots(s.getOperationTypeDefinitions()));
            case ObjectTypeExtensionDefinition o -> new Ast.Extension(object(o));
            case InterfaceTypeExtensionDefinition i -> new Ast.Extension(iface(i));
            case UnionTypeExtensionDefinition u -> new Ast.Extension(union(u));
            case EnumTypeExtensionDefinition e -> new Ast.Extension(enumDef(e));
            case InputObjectTypeExtensionDefinition i -> new Ast.Extension(input(i));
            case ScalarTypeExtensionDefinition s -> new Ast.Extension(new Ast.ScalarDef(null, s.getName(), directives(s.getDirectives())));
            case ObjectTypeDefinition o -> object(o);
            case InterfaceTypeDefinition i -> iface(i);
            case UnionTypeDefinition u -> union(u);
            case EnumTypeDefinition e -> enumDef(e);
            case InputObjectTypeDefinition i -> input(i);
            case ScalarTypeDefinition s -> new Ast.ScalarDef(desc(s.getDescription()), s.getName(), directives(s.getDirectives()));
            case DirectiveDefinition d -> new Ast.DirectiveDef(desc(d.getDescription()), d.getName(), inputValues(d.getInputValueDefinitions()),
                    d.isRepeatable(), d.getDirectiveLocations().stream().map(DirectiveLocation::getName).toList());
            default -> throw new IllegalArgumentException("unknown definition " + def.getClass().getSimpleName());
        };
    }

    private static List<Ast.RootOperation> roots(List<OperationTypeDefinition> ops) {
        return ops.stream().map(o -> new Ast.RootOperation(o.getName(), o.getTypeName().getName())).toList();
    }

    private static Ast.ObjectDef object(ObjectTypeDefinition o) {
        return new Ast.ObjectDef(desc(o.getDescription()), o.getName(), o.getImplements().stream().map(t -> ((TypeName) t).getName()).toList(),
                directives(o.getDirectives()), fields(o.getFieldDefinitions()));
    }

    private static Ast.InterfaceDef iface(InterfaceTypeDefinition o) {
        return new Ast.InterfaceDef(desc(o.getDescription()), o.getName(), o.getImplements().stream().map(t -> ((TypeName) t).getName()).toList(),
                directives(o.getDirectives()), fields(o.getFieldDefinitions()));
    }

    private static Ast.UnionDef union(UnionTypeDefinition u) {
        return new Ast.UnionDef(desc(u.getDescription()), u.getName(), directives(u.getDirectives()),
                u.getMemberTypes().stream().map(t -> ((TypeName) t).getName()).toList());
    }

    private static Ast.EnumDef enumDef(EnumTypeDefinition e) {
        return new Ast.EnumDef(desc(e.getDescription()), e.getName(), directives(e.getDirectives()),
                e.getEnumValueDefinitions().stream().map(v -> new Ast.EnumValueDef(desc(v.getDescription()), v.getName(), directives(v.getDirectives()))).toList());
    }

    private static Ast.InputDef input(InputObjectTypeDefinition i) {
        return new Ast.InputDef(desc(i.getDescription()), i.getName(), directives(i.getDirectives()), inputValues(i.getInputValueDefinitions()));
    }

    private static List<Ast.FieldDef> fields(List<FieldDefinition> fields) {
        return fields.stream().map(f -> new Ast.FieldDef(desc(f.getDescription()), f.getName(), inputValues(f.getInputValueDefinitions()),
                type(f.getType()), directives(f.getDirectives()))).toList();
    }

    private static List<Ast.InputValueDef> inputValues(List<InputValueDefinition> values) {
        return values.stream().map(v -> new Ast.InputValueDef(desc(v.getDescription()), v.getName(), type(v.getType()),
                v.getDefaultValue() == null ? null : value(v.getDefaultValue()), directives(v.getDirectives()))).toList();
    }

    private static List<Ast.Selection> selections(SelectionSet set) {
        if (set == null) return List.of();
        return set.getSelections().stream().map(s -> (Ast.Selection) switch (s) {
            case Field f -> new Ast.Field(loc(f), f.getAlias(), f.getName(), arguments(f.getArguments()), directives(f.getDirectives()), selections(f.getSelectionSet()));
            case FragmentSpread sp -> new Ast.Spread(loc(sp), sp.getName(), directives(sp.getDirectives()));
            case InlineFragment i -> new Ast.Inline(loc(i), i.getTypeCondition() == null ? null : i.getTypeCondition().getName(),
                    directives(i.getDirectives()), selections(i.getSelectionSet()));
            default -> throw new IllegalArgumentException(s.getClass().getSimpleName());
        }).toList();
    }

    private static List<Ast.Argument> arguments(List<Argument> args) {
        return args.stream().map(a -> new Ast.Argument(loc(a), a.getName(), value(a.getValue()))).toList();
    }

    private static List<Ast.Directive> directives(List<Directive> ds) {
        return ds.stream().map(d -> new Ast.Directive(loc(d), d.getName(), arguments(d.getArguments()))).toList();
    }

    private static Ast.Type type(Type<?> t) {
        return switch (t) {
            case TypeName n -> new Ast.NamedType(n.getName());
            case ListType l -> new Ast.ListType(type(l.getType()));
            case NonNullType n -> new Ast.NonNullType(type(n.getType()));
            default -> throw new IllegalArgumentException();
        };
    }

    private static Ast.Value value(Value<?> v) {
        return switch (v) {
            case VariableReference r -> new Ast.Variable(loc(r), r.getName());
            case IntValue i -> new Ast.IntV(i.getValue().toString());
            case FloatValue f -> new Ast.FloatV(f.getValue().toString());
            case StringValue s -> new Ast.StringV(s.getValue());
            case BooleanValue b -> new Ast.BooleanV(b.isValue());
            case NullValue n -> new Ast.NullV();
            case EnumValue e -> new Ast.EnumV(e.getName());
            case ArrayValue a -> new Ast.ListV(a.getValues().stream().map(Gj::value).toList());
            case ObjectValue o -> new Ast.ObjectV(o.getObjectFields().stream().map(f -> new Ast.ObjectField(f.getName(), value(f.getValue()))).toList());
            default -> throw new IllegalArgumentException();
        };
    }

    // ---- validation and execution

    public static GraphQLSchema schema(String sdl, Resolver resolver) {
        RuntimeWiring.Builder wiring = RuntimeWiring.newRuntimeWiring().wiringFactory(new graphql.schema.idl.WiringFactory() {
            @Override public boolean providesDataFetcher(graphql.schema.idl.FieldWiringEnvironment env) { return true; }
            @Override public graphql.schema.DataFetcher<?> getDataFetcher(graphql.schema.idl.FieldWiringEnvironment env) {
                String type = env.getParentType().getName(), field = env.getFieldDefinition().getName();
                return e -> resolver.resolve(type, field, e.getSource(), e.getArguments());
            }
            @Override public boolean providesTypeResolver(graphql.schema.idl.InterfaceWiringEnvironment env) { return true; }
            @Override public graphql.schema.TypeResolver getTypeResolver(graphql.schema.idl.InterfaceWiringEnvironment env) { return e -> e.getSchema().getObjectType(resolver.typeOf(e.getObject())); }
            @Override public boolean providesTypeResolver(graphql.schema.idl.UnionWiringEnvironment env) { return true; }
            @Override public graphql.schema.TypeResolver getTypeResolver(graphql.schema.idl.UnionWiringEnvironment env) { return e -> e.getSchema().getObjectType(resolver.typeOf(e.getObject())); }
        });
        var registry = new SchemaParser().parse(sdl);
        for (var scalar : registry.scalars().values())
            if (!graphql.schema.idl.ScalarInfo.isGraphqlSpecifiedScalar(scalar.getName()))
                wiring.scalar(graphql.schema.GraphQLScalarType.newScalar().name(scalar.getName()).definition(scalar)
                        .description(scalar.getDescription() == null ? null : scalar.getDescription().getContent())
                        .specifiedByUrl(scalar.getDirectives("specifiedBy").stream().map(d -> ((graphql.language.StringValue) d.getArgument("url").getValue()).getValue()).findFirst().orElse(null))
                        .coercing(new graphql.schema.Coercing<Object, Object>() {}).build());
        return new SchemaGenerator().makeExecutableSchema(registry, wiring.build());
    }

    /** The six rules of lab 17.4 in graphql-java's names. */
    public static final Map<String, OperationValidationRule> RULES = Map.of(
            "FieldsOnCorrectType", OperationValidationRule.FIELDS_ON_CORRECT_TYPE,
            "KnownArgumentNames", OperationValidationRule.KNOWN_ARGUMENT_NAMES,
            "NoUnusedVariables", OperationValidationRule.NO_UNUSED_VARIABLES,
            "NoFragmentCycles", OperationValidationRule.NO_FRAGMENT_CYCLES,
            "VariablesInAllowedPosition", OperationValidationRule.VARIABLE_TYPES_MATCH,
            "OverlappingFieldsCanBeMerged", OperationValidationRule.OVERLAPPING_FIELDS_CAN_BE_MERGED);

    /** Counts per rule, running only that rule. */
    public static Map<String, Integer> validate(GraphQLSchema schema, String query) {
        Document doc = parse(query);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (var e : RULES.entrySet()) {
            List<ValidationError> errors = new Validator().validateDocument(schema, doc, r -> r == e.getValue(), Locale.ENGLISH);
            if (!errors.isEmpty()) counts.put(e.getKey(), errors.size());
        }
        return counts;
    }

    /** Executes and returns the response as plain maps, as the course's engine does. */
    public static Map<String, Object> execute(GraphQLSchema schema, String query, String operationName, Map<String, Object> variables, Object root) {
        ExecutionInput.Builder in = ExecutionInput.newExecutionInput().query(query).operationName(operationName).root(root)
                .variables(variables == null ? Map.of() : variables);
        ExecutionResult r = GraphQL.newGraphQL(schema).build().execute(in);
        return r.toSpecification();
    }
}
