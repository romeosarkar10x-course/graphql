package dev.shelfie.filters;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

@Controller
class BookController {
    private final BookService service;

    BookController(BookService service) { this.service = service; }

    record BookList(List<Book> items, Long totalCount) {}

    /** totalCount costs a second query over every match, so it runs only when the client selects it. */
    @QueryMapping
    BookList books(@Argument BookFilter filter, @Argument BookOrder orderBy, @Argument int first,
                   DataFetchingFieldSelectionSet selection) {
        var items = service.find(filter, orderBy, first);
        Long total = selection.contains("totalCount") ? service.count(filter) : null;
        return new BookList(items, total);
    }

    @GraphQlExceptionHandler
    GraphQLError badFilter(BookService.BadFilterException e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(e.getMessage()).build();
    }
}
