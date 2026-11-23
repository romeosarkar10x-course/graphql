package dev.toolshed.inventory;

import dev.toolshed.common.BadInput;
import dev.toolshed.inventory.Model.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

@Controller
class InventoryController {
    static final int MAX_PAGE = 50;
    private final ToolRepository tools;

    InventoryController(ToolRepository tools) {
        this.tools = tools;
    }

    @QueryMapping
    Tool tool(@Argument ToolLookup by) {
        return (by.id() != null ? tools.byId(by.id()) : tools.byBarcode(by.barcode())).orElse(null);
    }

    @QueryMapping(name = "tools")
    ToolConnection toolPage(@Argument int first, @Argument String after, @Argument ToolFilter filter, @Argument ToolSort sort) {
        return tools.page(filter == null ? ToolFilter.NONE : filter, sort, pageSize(first), after);
    }

    @QueryMapping
    List<Category> categories() {
        return tools.categories();
    }

    @QueryMapping
    List<Branch> branches() {
        return tools.branches();
    }

    @QueryMapping
    List<SearchResult> search(@Argument String text, @Argument int first) {
        if (text.isBlank()) throw new BadInput("text", "search text must not be blank");
        return tools.search(text.strip(), pageSize(first));
    }

    /** Counted only when the client selects it: one extra statement. */
    @SchemaMapping(typeName = "ToolConnection")
    int totalCount(ToolConnection page) {
        return tools.count(page.filter());
    }

    // DataLoader through @BatchMapping: one statement for all the tools on a page.
    @BatchMapping(typeName = "Tool")
    Map<Tool, Category> category(List<Tool> page) {
        Map<String, Category> byId = index(tools.categoriesByIds(ids(page, Tool::categoryId)), Category::id);
        return page.stream().collect(Collectors.toMap(t -> t, t -> byId.get(t.categoryId()), (a, b) -> a, LinkedHashMap::new));
    }

    @BatchMapping(typeName = "Tool")
    Map<Tool, Branch> branch(List<Tool> page) {
        Map<String, Branch> byId = index(tools.branchesByIds(ids(page, Tool::branchId)), Branch::id);
        return page.stream().collect(Collectors.toMap(t -> t, t -> byId.get(t.branchId()), (a, b) -> a, LinkedHashMap::new));
    }

    @BatchMapping(typeName = "Category")
    Map<Category, Integer> toolCount(List<Category> categories) {
        Map<String, Integer> counts = tools.toolCounts(ids(categories, Category::id));
        return categories.stream().collect(Collectors.toMap(c -> c, c -> counts.getOrDefault(c.id(), 0), (a, b) -> a, LinkedHashMap::new));
    }

    /** The router's _entities call: all representations in one list, one statement. Unknown ids resolve to null. */
    @EntityMapping
    List<Tool> tool(@Argument List<String> idList) {
        Map<String, Tool> byId = index(tools.byIds(idList), Tool::id);
        return idList.stream().map(byId::get).toList();
    }

    static int pageSize(int first) {
        if (first < 1 || first > MAX_PAGE) throw new BadInput("first", "first must be between 1 and " + MAX_PAGE);
        return first;
    }

    private static <T> Set<String> ids(List<T> items, Function<T, String> id) {
        return items.stream().map(id).collect(Collectors.toSet());
    }

    private static <T> Map<String, T> index(List<T> items, Function<T, String> id) {
        return items.stream().collect(Collectors.toMap(id, t -> t));
    }
}
