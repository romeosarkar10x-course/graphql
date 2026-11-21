package dev.shelfie.boot;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** Provided: fields that throw. */
@Controller
public class LabController {
    @QueryMapping public String trouble(@Argument String kind) throws Exception { return Problems.trouble(kind); }
    @SchemaMapping(typeName = "Club", field = "trouble")
    public String clubTrouble(Data.Club club, @Argument String kind) throws Exception { return Problems.trouble(kind); }
}
