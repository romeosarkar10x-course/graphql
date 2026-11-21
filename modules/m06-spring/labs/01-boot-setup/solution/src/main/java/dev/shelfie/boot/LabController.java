package dev.shelfie.boot;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** Provided: the two fields the virtual-threads experiment needs. */
@Controller
public class LabController {
    @QueryMapping
    public String slowHello() throws InterruptedException {
        Thread.sleep(100);
        return "Hello, slowly";
    }

    @QueryMapping
    public String servedBy() { return Thread.currentThread().isVirtual() ? "virtual" : "platform"; }
}
