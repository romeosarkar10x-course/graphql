package dev.shelfie.perf;

import java.util.ArrayList;
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
 * Wraps the DataSource in datasource-proxy and counts every statement the database executes. While
 * recording (startRecording .. stopRecording), it also keeps each statement's SQL text, for tests.
 * One counter for the whole app: exact while requests run one at a time, as in tests.
 */
@Component
public class SqlCounter implements BeanPostProcessor {
    static final AtomicLong STATEMENTS = new AtomicLong();
    private static final List<String> RECORDED = new ArrayList<>();
    private static volatile boolean recording;

    /** Statements executed since the app started. */
    public static long statements() { return STATEMENTS.get(); }

    public static synchronized void startRecording() { RECORDED.clear(); recording = true; }

    /** Stops recording and returns the SQL of every statement since startRecording(), in order. */
    public static synchronized List<String> stopRecording() { recording = false; return List.copyOf(RECORDED); }

    private static synchronized void record(List<QueryInfo> queries) { if (recording) queries.forEach(q -> RECORDED.add(q.getQuery())); }

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (!(bean instanceof DataSource ds) || bean instanceof ProxyDataSource) return bean;
        return ProxyDataSourceBuilder.create(ds).name("shelfie").listener(new QueryExecutionListener() {
            @Override public void beforeQuery(ExecutionInfo exec, List<QueryInfo> queries) {}
            @Override public void afterQuery(ExecutionInfo exec, List<QueryInfo> queries) {
                STATEMENTS.addAndGet(queries.size());
                record(queries);
            }
        }).build();
    }
}
