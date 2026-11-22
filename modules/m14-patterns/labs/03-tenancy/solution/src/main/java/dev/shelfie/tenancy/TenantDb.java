package dev.shelfie.tenancy;

import java.util.function.Function;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Every query runs inside a transaction that first becomes the tenant role and sets app.tenant. Both are LOCAL:
 * they end with the transaction, so a pooled connection never carries one tenant's setting into another's request.
 * Row-level security then filters every table that has a policy, whatever SQL the code above writes.
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
        return tx.execute(status -> {
            jdbc.sql("SET LOCAL ROLE m14_tenant").update();
            jdbc.sql("SELECT set_config('app.tenant', ?, true)").param(tenant.id()).query().singleValue();
            return work.apply(jdbc);
        });
    }
}
