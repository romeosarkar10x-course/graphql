package dev.shelfie.boot;

import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Turns the schema inspection report from a log line into a failed startup. */
@Configuration
public class InspectionConfig {
    @Bean
    GraphQlSourceBuilderCustomizer failOnSchemaMismatch() {
        return builder -> builder.inspectSchemaMappings(report -> {
            boolean dirty = !report.unmappedFields().isEmpty() || !report.unmappedRegistrations().isEmpty()
                || !report.unmappedArguments().isEmpty() || !report.fieldNullnessErrors().isEmpty()
                || !report.argumentNullnessErrors().isEmpty() || !report.skippedTypes().isEmpty();
            if (dirty) throw new IllegalStateException("Schema and code disagree:\n" + report);
        });
    }
}
