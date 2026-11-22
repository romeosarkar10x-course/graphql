package dev.shelfie.fed.accounts;

import dev.shelfie.fed.Data;
import dev.shelfie.fed.Data.User;
import dev.shelfie.fed.Stats;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@Profile("accounts")
class AccountsController {
    @QueryMapping
    User me(@ContextValue(name = "userId", required = false) String userId) {
        return userId == null ? null : Data.USERS.get(userId);
    }

    @QueryMapping
    User user(@Argument String id) {
        return Data.USERS.get(id);
    }

    @QueryMapping
    List<User> users() {
        return List.copyOf(Data.USERS.values());
    }

    /** One call for every User representation in an _entities request. */
    @EntityMapping
    List<User> user(@Argument List<String> idList) {
        Stats.entity("User", idList.size());
        return idList.stream().map(Data.USERS::get).toList();
    }
}
