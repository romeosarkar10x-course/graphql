package probe;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

// Probe: does a @RestController on /graphql with produces=multipart/mixed win over Spring for GraphQL's router?
@RestController
class Multipart {
  @PostMapping(value = "/graphql", produces = "multipart/mixed")
  String mp() { return "--- from the @RestController ---"; }
}
