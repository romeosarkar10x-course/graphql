package dev.shelfie.boot;

import java.time.Instant;
import java.time.ZoneId;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class LabController {
    /** Three levels below the root, and it still sees the request's tenant: GraphQLContext is request-wide. */
    @SchemaMapping
    public String startsAtLocal(Data.Meetup meetup, @ContextValue String tenant) {
        String zone = TenantInterceptor.ZONES.getOrDefault(tenant, "UTC");
        return Instant.parse(meetup.startsAt()).atZone(ZoneId.of(zone)).toString();
    }

    @SchemaMapping
    public String headline(Data.Meetup meetup) { return meetup.title(); }
}
