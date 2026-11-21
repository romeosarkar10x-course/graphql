package dev.shelfie.perf;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Wraps the DataSource in datasource-proxy and counts every statement the database executes.
 * One counter for the whole app: the per-request numbers are exact while requests run one at a time.
 */
@Component
class SqlCounter implements BeanPostProcessor {
    static final AtomicLong STATEMENTS = new AtomicLong();

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (!(bean instanceof DataSource ds) || bean instanceof ProxyDataSource) return bean;
        return ProxyDataSourceBuilder.create(ds).name("shelfie").listener(new QueryExecutionListener() {
            @Override public void beforeQuery(ExecutionInfo exec, List<QueryInfo> queries) {}
            @Override public void afterQuery(ExecutionInfo exec, List<QueryInfo> queries) { STATEMENTS.addAndGet(queries.size()); }
        }).build();
    }
}
