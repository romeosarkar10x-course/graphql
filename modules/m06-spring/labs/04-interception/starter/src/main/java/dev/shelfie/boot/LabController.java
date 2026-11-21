package dev.shelfie.boot;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class LabController {
    /**
     * TODO: startsAt in the tenant's zone. The tenant comes from the X-Shelfie-Tenant header, which this
     * method cannot see: a WebGraphQlInterceptor has to put it into GraphQLContext first.
     */
    @SchemaMapping
    public String startsAtLocal(Data.Meetup meetup) { return meetup.startsAt(); }

    /** Returns the title as it is; @uppercase (lab.graphqls) must be applied by wiring, not here. */
    @SchemaMapping
    public String headline(Data.Meetup meetup) { return meetup.title(); }
}
