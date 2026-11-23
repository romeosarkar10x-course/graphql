package dev.toolshed.lending;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/** The toolshed.* block of application.yaml. */
@ConfigurationProperties("toolshed")
public record LendingProperties(int loanLimit, int maxDays, Jwt jwt) {
    public record Jwt(Resource publicKey, String issuer, String audience) {}
}
