package dev.toolshed.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import java.util.List;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Counts every SQL statement the subgraph runs, as the counter toolshed.db.statements
 * (/actuator/metrics/toolshed.db.statements, toolshed_db_statements_total in Prometheus). The N+1 check reads
 * it before and after one request: the difference must not grow with the page size.
 */
public class SqlCounter implements BeanPostProcessor {
    private static final Counter STATEMENTS = Counter.builder("toolshed.db.statements")
            .description("SQL statements executed").register(Metrics.globalRegistry);

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (!(bean instanceof DataSource ds) || bean instanceof ProxyDataSource) return bean;
        return ProxyDataSourceBuilder.create(ds).name(name).listener(new QueryExecutionListener() {
            @Override
            public void beforeQuery(ExecutionInfo exec, List<QueryInfo> queries) {}

            @Override
            public void afterQuery(ExecutionInfo exec, List<QueryInfo> queries) {
                STATEMENTS.increment(queries.size());
            }
        }).build();
    }
}
