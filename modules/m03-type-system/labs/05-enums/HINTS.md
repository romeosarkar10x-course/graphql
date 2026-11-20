# Hints, lab 3.5

1. `.type("Visibility", t -> t.enumValues(name -> ...))`: the provider gets the GraphQL name and returns the Java value. graphql-java uses the same mapping both ways, so a fetcher returning `"X"` serialises as `PRIVATE`, and the argument `PRIVATE` arrives in your fetcher as `"X"`.
2. `new NaturalEnumValuesProvider<>(Enums.ReadingStatus.class)` maps names to constants of the same name. A Java constant with no matching GraphQL value cannot be serialised: that is the v1 error, and it is correct behaviour.
3. Jackson 3 moved enum options to `tools.jackson.databind.cfg.EnumFeature`. Look for one that uses a default value for unknown names, and an annotation from `com.fasterxml.jackson.annotation` that marks which constant is the default.
