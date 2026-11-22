package dev.shelfie.mini;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import dev.shelfie.harness.Gj;
import dev.shelfie.harness.Known;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

/**
 * Your parser against graphql-java's on inputs Jazzer makes up. A crash in your parser (anything but a SyntaxError)
 * is a finding and stops the run. A disagreement (one accepts, the other rejects, or the trees differ) is written
 * to fuzz/disagreements/ and the run goes on, so you can investigate each one afterwards.
 *
 *   JAZZER_FUZZ=1 mvn test -Dtest=ParserFuzzTest      fuzz for maxDuration; without JAZZER_FUZZ it replays the corpus
 */
class ParserFuzzTest {
    @FuzzTest(maxDuration = "60s")
    void parsersAgree(FuzzedDataProvider data) throws Exception {
        String doc = data.consumeRemainingAsString();
        String mine, theirs;
        Ast.Document tree = null;
        try { tree = Parser.parse(doc); mine = tree.toString(); } catch (SyntaxError e) { mine = "REJECT"; }
        Ast.Document other = null;
        try { other = Gj.toAst(Gj.parse(doc)); theirs = other.toString(); } catch (graphql.parser.InvalidSyntaxException | IllegalArgumentException e) { theirs = "REJECT"; }
        if (mine.equals(theirs) || normal(mine).equals(normal(theirs))) return;
        if (Known.parser(doc, tree, other) != null) return;   // graphql-java departs from the spec here
        Path dir = Path.of("fuzz/disagreements");
        Files.createDirectories(dir);
        byte[] bytes = doc.getBytes(StandardCharsets.UTF_8);
        String name = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(bytes)).substring(0, 12);
        Files.write(dir.resolve(name + ".graphql"), bytes);
        Files.writeString(dir.resolve(name + ".txt"), "yours:        " + mine.replace("\n", "\\n") + "\ngraphql-java: " + theirs.replace("\n", "\\n") + "\n");
    }

    /** Numbers as graphql-java keeps them (BigInteger, BigDecimal): 1e10 and 1E+10 are the same value. */
    static String normal(String ast) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(IntV|FloatV)\\[text=([^\\]]+)]").matcher(ast);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String n = m.group(1).equals("IntV") ? new java.math.BigInteger(m.group(2)).toString() : new java.math.BigDecimal(m.group(2)).toString();
            m.appendReplacement(sb, m.group(1) + "[text=" + n + "]");
        }
        return m.appendTail(sb).toString();
    }
}
