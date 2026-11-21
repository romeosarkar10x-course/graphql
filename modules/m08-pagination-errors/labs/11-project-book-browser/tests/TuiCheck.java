import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Mini-project 8 harness: drives dev.shelfie.tui.App on a 60x15 virtual terminal (13 list rows) against
 * FeedServer, reads the screen back, and checks what a user would see.
 *   TuiCheck <url> fixed     200 books, no bot
 *   TuiCheck <url> live      a bot adds books while we scroll
 */
public class TuiCheck {
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "ok    " : "FAIL  ") + what); if (!ok) fails++; }
    static final Pattern ROW = Pattern.compile("^(> |  )#(\\d+) .+ - .+$");

    static DefaultVirtualTerminal term;

    static List<String> screen() {
        var rows = new ArrayList<String>();
        for (int r = 0; r < 15; r++) {
            var sb = new StringBuilder();
            for (int c = 0; c < 60; c++) sb.append(term.getBufferCharacter(c, r).getCharacterString());
            rows.add(sb.toString().stripTrailing());
        }
        return rows;
    }

    /** Waits until the screen has not changed for 150 ms (at most 5 s). */
    static List<String> settle() throws InterruptedException {
        var last = screen();
        long stableSince = System.currentTimeMillis(), deadline = stableSince + 5000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
            var now = screen();
            if (!now.equals(last)) { last = now; stableSince = System.currentTimeMillis(); }
            else if (System.currentTimeMillis() - stableSince >= 150) break;
        }
        return last;
    }

    static List<String> press(KeyType type, int times) throws InterruptedException {
        for (int i = 0; i < times; i++) { term.addInput(new KeyStroke(type)); Thread.sleep(15); }
        return settle();
    }

    static String selected(List<String> s) { return s.stream().filter(l -> l.startsWith("> ")).findFirst().orElse(""); }
    static long idOf(String row) { var m = ROW.matcher(row); return m.matches() ? Long.parseLong(m.group(2)) : -1; }
    static List<Long> visibleIds(List<String> s) { return s.subList(1, s.size() - 1).stream().map(TuiCheck::idOf).filter(i -> i > 0).toList(); }

    static int requests(String url) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(url.replace("/graphql", "/stats"))).GET().build();
        var body = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).body();
        return Integer.parseInt(body.replaceAll(".*\"requests\":(\\d+).*", "$1"));
    }

    public static void main(String[] args) throws Exception {
        String url = args[0], mode = args[1];
        term = new DefaultVirtualTerminal(new TerminalSize(60, 15));
        var errors = new ArrayList<Throwable>();
        var app = new Thread(() -> { try { dev.shelfie.tui.App.run(term, url); } catch (Throwable t) { errors.add(t); } }, "app");
        app.start();
        for (int i = 0; i < 200 && screen().get(14).isEmpty() && app.isAlive(); i++) Thread.sleep(50);   // first draw (JVM warm-up)
        var s = settle();

        if (mode.equals("fixed")) {
            check(s.get(0).startsWith("Shelfie books  20 loaded"), "row 0 says 20 loaded: '" + s.get(0) + "'");
            check(s.get(14).equals(dev.shelfie.tui.App.FOOTER), "row 14 is the footer");
            check(idOf(selected(s)) == 200 && selected(s).equals(s.get(1)), "the newest book (#200) is selected on row 1");
            check(visibleIds(s).equals(List.of(200L, 199L, 198L, 197L, 196L, 195L, 194L, 193L, 192L, 191L, 190L, 189L, 188L)),
                "13 rows of books, newest first: #200 to #188");
            s = press(KeyType.ArrowUp, 1);
            check(idOf(selected(s)) == 200, "Up at the top stays on #200");
            s = press(KeyType.ArrowDown, 16);
            check(idOf(selected(s)) == 184 && selected(s).equals(s.get(13)), "16 x Down: #184 selected, on the last list row (the list scrolled)");
            check(s.get(0).startsWith("Shelfie books  40 loaded"), "a second page loaded before the user reached the end: 40 loaded");
            s = press(KeyType.PageDown, 1);
            check(idOf(selected(s)) == 171, "PgDn moves 13 rows: #171");
            s = press(KeyType.PageUp, 2);
            check(idOf(selected(s)) == 197, "PgUp twice: #197");
            s = press(KeyType.Home, 1);
            check(idOf(selected(s)) == 200 && s.get(1).startsWith("> "), "Home: back to #200 on row 1");
            s = press(KeyType.PageDown, 20);
            check(idOf(selected(s)) == 1 && s.get(0).contains("200 loaded"), "PgDn to the end: #1 selected, 200 loaded");
            int used = requests(url);
            check(used <= 11, "200 books in at most 11 requests (" + used + "): pages load once, by cursor");
        } else {
            var seen = new ArrayList<Long>();
            for (int i = 0; i < 12; i++) {
                s = press(KeyType.PageDown, 1);
                seen.addAll(visibleIds(s));
                Thread.sleep(120);   // the bot posts meanwhile
            }
            var ids = new ArrayList<>(new java.util.LinkedHashSet<>(seen));
            boolean newestFirst = true;
            for (int i = 1; i < ids.size(); i++) if (ids.get(i) >= ids.get(i - 1)) newestFirst = false;
            check(newestFirst && ids.size() > 100, "while books keep arriving, every screen continues the list: no repeats, strictly older (" + ids.size() + " books)");
            var visible = visibleIds(s);
            check(new HashSet<>(visible).size() == visible.size(), "no book twice on one screen");
        }

        term.addInput(new KeyStroke('q', false, false));
        app.join(3000);
        check(!app.isAlive(), "q quits (run returns)");
        check(errors.isEmpty(), "no exception from App.run" + (errors.isEmpty() ? "" : ": " + errors.getFirst()));
        System.exit(fails == 0 ? 0 : 1);
    }
}
