package dev.toolshed.lending;

import dev.toolshed.lending.Model.Loan;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import reactor.core.publisher.Mono;

/** The open loan of each tool on a page, in one statement (Tool.currentLoan). */
@Configuration
class Loaders {
    Loaders(BatchLoaderRegistry loaders, LendingRepository db) {
        loaders.forTypePair(String.class, Loan.class).registerMappedBatchLoader((toolIds, env) ->
                Mono.fromCallable(() -> (Map<String, Loan>) db.openLoansByTool(toolIds)));
    }
}
