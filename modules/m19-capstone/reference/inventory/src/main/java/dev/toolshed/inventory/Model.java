package dev.toolshed.inventory;

import java.time.OffsetDateTime;
import java.util.List;

/** The inventory's types. Record and class names match the GraphQL type names, so interfaces and unions resolve by class. */
final class Model {
    private Model() {}

    enum Condition { NEW, GOOD, WORN, NEEDS_REPAIR }

    enum ToolSort { NAME_ASC, NAME_DESC, NEWEST }

    /** A member of the SearchResult union. */
    sealed interface SearchResult permits Tool, Category, Branch {}

    record Tool(String id, String barcode, String name, Condition condition, String categoryId, String branchId,
                OffsetDateTime addedAt, ToolSpecs specs) implements SearchResult {}

    sealed interface ToolSpecs permits PowerSpecs, HandSpecs {
        String summary();
    }

    record PowerSpecs(int volts, boolean cordless) implements ToolSpecs {
        public String summary() {
            return volts + " V, " + (cordless ? "cordless" : "mains");
        }
    }

    record HandSpecs(int weightGrams) implements ToolSpecs {
        public String summary() {
            return weightGrams >= 1000 ? String.format("%.1f kg", weightGrams / 1000.0) : weightGrams + " g";
        }
    }

    record Category(String id, String name) implements SearchResult {}

    record Branch(String id, String name, String address, String openingHours) implements SearchResult {}

    /** The @oneOf input: GraphQL guarantees exactly one field is set. */
    record ToolLookup(String id, String barcode) {}

    record ToolFilter(String categoryId, String branchId, List<Condition> condition, String nameContains, Boolean powered) {
        static final ToolFilter NONE = new ToolFilter(null, null, null, null, null);
    }

    record PageInfo(boolean hasNextPage, String endCursor) {}

    record ToolEdge(String cursor, Tool node) {}

    /** ToolConnection. The filter is kept so totalCount can be counted, and only when a client asks for it. */
    record ToolConnection(List<ToolEdge> edges, PageInfo pageInfo, ToolFilter filter) {}
}
