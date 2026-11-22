package dev.shelfie.tenancy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** A tenant's plan, read under its own tenant (the tenants table has a policy too) and cached per tenant. */
@Component
public class Plans {
    private final TenantDb db;
    private final Map<String, String> byTenant = new ConcurrentHashMap<>();

    Plans(TenantDb db) {
        this.db = db;
    }

    public String of(Tenant tenant) {
        return byTenant.computeIfAbsent(tenant.id(), id -> db.as(tenant,
            jdbc -> jdbc.sql("SELECT plan FROM m14_03.tenants").query(String.class).optional().orElse("basic")));
    }
}
