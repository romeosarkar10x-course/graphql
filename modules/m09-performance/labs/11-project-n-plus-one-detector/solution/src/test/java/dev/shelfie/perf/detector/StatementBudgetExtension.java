package dev.shelfie.perf.detector;

import dev.shelfie.perf.SqlCounter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * The N+1 detector. Before the test method runs (after @BeforeEach), it notes the statement count and
 * starts recording SQL; after the method, it compares. Over budget fails the test with the count, the
 * budget and the statements that repeat most: a statement repeated once per parent is the N+1.
 */
public class StatementBudgetExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(StatementBudgetExtension.class);

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        if (budget(context).isEmpty()) return;
        context.getStore(NS).put("start", SqlCounter.statements());
        SqlCounter.startRecording();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        var budget = budget(context);
        if (budget.isEmpty()) return;
        long ran = SqlCounter.statements() - context.getStore(NS).get("start", Long.class);
        List<String> sql = SqlCounter.stopRecording();
        if (context.getExecutionException().isPresent()) return;   // the test's own failure is the one to report
        if (ran > budget.get()) throw new AssertionError(message(ran, budget.get(), sql));
    }

    /** The method's budget, else the nearest class's (nested classes inherit their outer class's). */
    static Optional<Integer> budget(ExtensionContext context) {
        Optional<MaxStatements> onMethod = context.getTestMethod().flatMap(m -> AnnotationSupport.findAnnotation(m, MaxStatements.class));
        if (onMethod.isPresent()) return onMethod.map(MaxStatements::value);
        for (Optional<Class<?>> c = context.getTestClass(); c.isPresent(); c = Optional.ofNullable(c.get().getEnclosingClass())) {
            var onClass = AnnotationSupport.findAnnotation(c.get(), MaxStatements.class);
            if (onClass.isPresent()) return onClass.map(MaxStatements::value);
        }
        return Optional.empty();
    }

    static String message(long ran, int budget, List<String> sql) {
        Map<String, Long> repeats = sql.stream().map(s -> s.replaceAll("\\s+", " ").trim())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String top = repeats.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(3)
            .map(e -> "  " + e.getValue() + "x " + (e.getKey().length() > 160 ? e.getKey().substring(0, 160) + "..." : e.getKey()))
            .collect(Collectors.joining("\n"));
        return "ran " + ran + " SQL statements, budget " + budget + ". Most repeated:\n" + top;
    }
}
