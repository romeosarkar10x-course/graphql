package dev.shelfie.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

/**
 * Mini-project 8: a terminal book browser over the feed connection.
 *   java -jar target/book-browser.jar [url]      (default http://localhost:4000/graphql)
 *
 * Screen: row 0 "Shelfie books  N loaded"; rows 1..h-2 the books, "#id title - author", the selected one
 * starting with "> " and the others with two spaces; row h-1 "Up/Down move  PgUp/PgDn page  Home top  q quit".
 */
public final class App {
    public static final String FOOTER = "Up/Down move  PgUp/PgDn page  Home top  q quit";

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://localhost:4000/graphql";
        try (Terminal terminal = new DefaultTerminalFactory().createTerminal()) {
            run(terminal, url);
        }
    }

    /** Draws, reads keys, and returns when the user presses q. The test calls this with a virtual terminal. */
    public static void run(Terminal terminal, String url) throws Exception {
        var client = new FeedClient(url);
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();
        try {
            var browser = new Browser(client::page, listRows(screen.getTerminalSize()));
            draw(screen, browser);
            while (true) {
                // pollInput, not readInput: Lanterna 3.1.5's virtual terminal (the test's) holds its lock
                // while readInput waits, and nothing else can read the screen meanwhile.
                KeyStroke key = screen.pollInput();
                if (key == null) { Thread.sleep(10); continue; }
                if (key.getKeyType() == KeyType.EOF) return;
                if (key.getKeyType() == KeyType.Character && key.getCharacter() == 'q') return;
                switch (key.getKeyType()) {
                    case ArrowDown -> browser.down();
                    case ArrowUp -> browser.up();
                    case PageDown -> browser.pageDown();
                    case PageUp -> browser.pageUp();
                    case Home -> browser.home();
                    default -> {}
                }
                var size = screen.doResizeIfNecessary();
                if (size != null) browser.resize(listRows(size));
                draw(screen, browser);
            }
        } finally {
            screen.stopScreen();
        }
    }

    static int listRows(TerminalSize size) { return Math.max(1, size.getRows() - 2); }

    static void draw(Screen screen, Browser b) throws Exception {
        screen.clear();
        var g = screen.newTextGraphics();
        var size = screen.getTerminalSize();
        int width = size.getColumns(), rows = listRows(size);
        g.putString(0, 0, fit("Shelfie books  " + b.books().size() + " loaded" + (b.hasMore() ? "" : "  (end)"), width));
        for (int r = 0; r < rows; r++) {
            int i = b.top() + r;
            if (i >= b.books().size()) break;
            var book = b.books().get(i);
            g.putString(0, 1 + r, fit((i == b.selected() ? "> " : "  ") + "#" + book.id() + " " + book.title() + " - " + book.author(), width));
        }
        g.putString(0, size.getRows() - 1, fit(FOOTER, width));
        screen.refresh();
    }

    static String fit(String s, int width) { return s.length() <= width ? s : s.substring(0, Math.max(0, width)); }
}
