package dev.shelfie.sdl;

/** java -jar target/records-to-sdl.jar 'dev.shelfie.fixtures.Shelfie$Query' [more roots...] */
public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: Main <root class>...   e.g. 'dev.shelfie.fixtures.Shelfie$Query'");
            System.exit(2);
        }
        Class<?>[] roots = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) roots[i] = Class.forName(args[i]);
        try {
            System.out.print(SdlGenerator.generate(roots));
        } catch (IllegalArgumentException e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
    }
}
