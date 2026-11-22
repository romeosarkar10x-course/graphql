package dev.shelfie.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;

class ProbeFuzzTest {
    @FuzzTest(maxDuration = "20s")
    void graphqlJavaParserDoesNotCrash(FuzzedDataProvider data) {
        try { new Parser().parseDocument(data.consumeRemainingAsString()); } catch (InvalidSyntaxException expected) { }
    }
}
