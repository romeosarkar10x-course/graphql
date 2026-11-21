package dev.shelfie.tiny;

import java.util.List;

/** Finds the @GraphQLController classes in a package, from directories and jars on the class path. */
final class Scanner {
    private Scanner() {}

    /**
     * TODO: ClassLoader.getResources(package path) gives one URL per class-path entry that has the package:
     * "file:" for target/classes, "jar:" when running from the jar (java -jar target/tiny-graphql.jar puts
     * the app in a jar). List the classes under it and keep those annotated @GraphQLController.
     */
    static List<Class<?>> controllers(String pkg) {
        return List.of();
    }
}
