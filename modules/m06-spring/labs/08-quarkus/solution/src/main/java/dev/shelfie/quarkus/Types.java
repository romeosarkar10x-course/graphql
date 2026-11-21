package dev.shelfie.quarkus;

import java.util.List;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;

/** The GraphQL types, as Java: SmallRye derives the schema from these classes, their fields and descriptions. */
public final class Types {
    private Types() {}

    public static class Club {
        @Id @NonNull public String id;
        @NonNull public String name;
        public int memberCount;
        /** Not part of the schema: @Ignore-free because it is not public. Nested fields use it. */
        String ownerId;
        String currentBookId;

        static Club from(Data.Club c) {
            var x = new Club();
            x.id = c.id(); x.name = c.name(); x.memberCount = c.memberIds().size();
            x.ownerId = c.ownerId(); x.currentBookId = c.currentBookId();
            return x;
        }
    }

    public static class User {
        @Id @NonNull public String id;
        @NonNull public String name;
        @NonNull public String handle;

        static User from(Data.User u) { var x = new User(); x.id = u.id(); x.name = u.name(); x.handle = u.handle(); return x; }
    }

    public static class Book {
        @Id @NonNull public String id;
        @NonNull public String title;
        @NonNull public List<@NonNull String> authors;

        static Book from(Data.Book b) { var x = new Book(); x.id = b.id(); x.title = b.title(); x.authors = b.authors(); return x; }
    }
}
