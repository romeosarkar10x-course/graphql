package dev.shelfie.mini;

import dev.shelfie.mini.Ast.Document;
import dev.shelfie.mini.Ast.Loc;
import java.util.ArrayList;
import java.util.List;

/**
 * Lab 17.4: six of the spec's validation rules. Report each violation as a Problem whose rule is one of
 * FieldsOnCorrectType (5.3.1), OverlappingFieldsCanBeMerged (5.3.2), KnownArgumentNames (5.4.1),
 * NoFragmentCycles (5.5.2.2), NoUnusedVariables (5.8.4) and VariablesInAllowedPosition (5.8.5).
 */
public final class Validator {
    public record Problem(String rule, String message, List<Loc> locations) {}

    private Validator() {}

    public static List<Problem> validate(Schema schema, Document doc) {
        List<Problem> problems = new ArrayList<>();
        // TODO lab 17.4: the six rules
        return problems;
    }
}
