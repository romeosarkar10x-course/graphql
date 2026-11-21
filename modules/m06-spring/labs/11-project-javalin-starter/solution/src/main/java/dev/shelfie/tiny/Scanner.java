package dev.shelfie.tiny;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finds the @GraphQLController classes in a package, from directories and jars on the class path. */
final class Scanner {
    private Scanner() {}

    static List<Class<?>> controllers(String pkg) {
        String path = pkg.replace('.', '/');
        var names = new ArrayList<String>();
        try {
            for (URL url : Collections.list(Thread.currentThread().getContextClassLoader().getResources(path))) {
                if (url.getProtocol().equals("file")) walk(new File(url.toURI()), pkg, names);
                else if (url.getProtocol().equals("jar"))
                    try (var jar = ((JarURLConnection) url.openConnection()).getJarFile()) {
                        jar.stream().map(e -> e.getName())
                            .filter(n -> n.startsWith(path + "/") && n.endsWith(".class") && !n.contains("$"))
                            .forEach(n -> names.add(n.substring(0, n.length() - 6).replace('/', '.')));
                    }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        var found = new ArrayList<Class<?>>();
        for (String n : names) {
            try {
                Class<?> c = Class.forName(n, false, Thread.currentThread().getContextClassLoader());
                if (c.isAnnotationPresent(GraphQLController.class)) found.add(c);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return found;
    }

    private static void walk(File dir, String pkg, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) walk(f, pkg + "." + f.getName(), out);
            else if (f.getName().endsWith(".class") && !f.getName().contains("$"))
                out.add(pkg + "." + f.getName().substring(0, f.getName().length() - 6));
        }
    }
}
