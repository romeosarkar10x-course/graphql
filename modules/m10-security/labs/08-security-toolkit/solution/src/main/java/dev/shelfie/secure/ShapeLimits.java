package dev.shelfie.secure;

import graphql.ExecutionInput;
import graphql.GraphqlErrorBuilder;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.language.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lab 10.8: limits on a document's shape that QueryComplexityLimits does not count, from graphql-cop's
 * findings. At most 30 aliases (Alias Overloading sends 101), at most 10 directives and no directive
 * repeated on one element (Directive Overloading sends 10 unknown ones, and graphql-java would report all
 * 10). One error, whatever the document had: error lists are work too.
 */
@Configuration
public class ShapeLimits implements PreparsedDocumentProvider {
  static final int MAX_ALIASES = 30, MAX_DIRECTIVES = 10;

  @Bean
  GraphQlSourceBuilderCustomizer shapeLimitsProvider() {
    return b -> b.configureGraphQl(g -> g.preparsedDocumentProvider(this));
  }

  @Override
  public CompletableFuture<PreparsedDocumentEntry> getDocumentAsync(ExecutionInput input, Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidate) {
    PreparsedDocumentEntry entry = parseAndValidate.apply(input);
    if (entry.getDocument() == null) return CompletableFuture.completedFuture(entry);
    int[] aliases = {0}, directives = {0};
    boolean[] repeated = {false};
    new NodeTraverser().preOrder(new NodeVisitorStub() {
      @Override
      public graphql.util.TraversalControl visitField(Field f, graphql.util.TraverserContext<Node> c) {
        if (f.getAlias() != null) aliases[0]++;
        return super.visitField(f, c);
      }

      @Override
      protected graphql.util.TraversalControl visitNode(Node node, graphql.util.TraverserContext<Node> c) {
        if (node instanceof DirectivesContainer<?> dc && !(node instanceof DirectiveDefinition)) {
          List<Directive> ds = dc.getDirectives();
          directives[0] += ds.size();
          if (ds.stream().map(Directive::getName).distinct().count() < ds.size()) repeated[0] = true;
        }
        return graphql.util.TraversalControl.CONTINUE;
      }
    }, entry.getDocument());
    String problem = aliases[0] > MAX_ALIASES ? aliases[0] + " aliases (at most " + MAX_ALIASES + ")"
        : directives[0] > MAX_DIRECTIVES ? directives[0] + " directives (at most " + MAX_DIRECTIVES + ")"
        : repeated[0] ? "a directive repeated on one element" : null;
    if (problem == null) return CompletableFuture.completedFuture(entry);
    return CompletableFuture.completedFuture(new PreparsedDocumentEntry(GraphqlErrorBuilder.newError()
        .message("document refused: " + problem).extensions(Map.of("code", "DOCUMENT_TOO_COMPLEX")).build()));
  }
}
