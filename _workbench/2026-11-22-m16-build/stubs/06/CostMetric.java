package dev.shelfie.ops;

import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import org.springframework.stereotype.Component;

/**
 * Module 16.6: what a request costs, before it runs, as a metric an autoscaler can use.
 *
 * Cost: every field selected costs 1, times the number of items its parents return. A list field returns `first`
 * items when it has that argument, otherwise its expected size: Query.books 10, Query.clubs 3, Query.search 3,
 * Club.members 6, Club.readingList 4, Book.reviews 5, Book.similar 3 (other lists: 1).
 * So  { books(first: 2) { title similar { title } } }  costs  1 + 2 + 2 + 2*3 = 11.
 *
 *   graphql_cost_sum / graphql_cost_count{graphql_client_name, graphql_operation_name}   (a DistributionSummary)
 *   "extensions": {"cost": 11}                                                         on every response
 */
@Component
public class CostMetric extends SimplePerformantInstrumentation {
    // TODO 16.6
}
