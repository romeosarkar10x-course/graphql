# Hints, lab 9.4

1. `selection.contains("members/reviews/book")` tells you what's selected anywhere below the field;
   `selection.getFields("members").getFirst().getArguments().get("first")` gives the argument (defaults
   already applied). `getFields(...)` returning more than one field means aliases.
2. `DSL.field(DSL.name("c", "id"))` is a column of the table aliased `c`;
   `multiset(select(...).from(...).where(field(name("m", "club_id")).eq(field(name("c", "id")))))` is
   correlated with the outer row. Pass a `List<SelectField<?>>` to `select(...)` to build the column
   list as you go, and read nested results with `record.get(multisetField)`.
3. If the child fields still run their loaders, check that the parents you return are the ones that
   carry the prefetched values, and that every loader method returns them before it touches SQL. Count
   with `extensions.sql.statements`: the dashboard should report 1.
