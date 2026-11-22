package dev.shelfie.tenancy;

import java.util.function.Function;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a resolver's SQL in a transaction, as the owner. Isolation is up to every WHERE clause.
 * TODO become the tenant role and set app.tenant for the transaction, so the database filters every path.
 */
@Component
public class TenantDb {
    private final JdbcClient jdbc;
    private final TransactionTemplate tx;

    TenantDb(JdbcClient jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    public <T> T as(Tenant tenant, Function<JdbcClient, T> work) {
        return tx.execute(status -> work.apply(jdbc));
    }
}
