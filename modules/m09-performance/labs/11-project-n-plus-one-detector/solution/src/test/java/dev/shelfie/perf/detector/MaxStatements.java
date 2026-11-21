package dev.shelfie.perf.detector;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Fails the test if its body runs more than {@code value} SQL statements. On a class it is the budget of
 * every test in it; on a method it overrides the class's. Only the test method counts, not @BeforeEach.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(StatementBudgetExtension.class)
public @interface MaxStatements {
    int value();
}
