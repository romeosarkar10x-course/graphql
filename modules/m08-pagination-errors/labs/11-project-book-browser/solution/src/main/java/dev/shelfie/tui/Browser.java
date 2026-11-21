package dev.shelfie.tui;

import java.util.ArrayList;
import java.util.List;

/**
 * The browser's state, without any terminal: the books loaded so far, the selection, and the first
 * visible row. Pages load by cursor when the selection gets within LOOKAHEAD rows of the end, so the
 * list never shows a book twice, however many books arrive while the user reads.
 */
public final class Browser {
    public static final int PAGE = 20, LOOKAHEAD = 5;

    public interface Source { FeedClient.Page page(int first, String after) throws Exception; }

    private final Source source;
    private final List<FeedClient.Book> books = new ArrayList<>();
    private String cursor;
    private boolean more = true;
    private int selected, top, rows;

    public Browser(Source source, int rows) throws Exception {
        this.source = source;
        this.rows = rows;
        load();
    }

    private void load() throws Exception {
        if (!more) return;
        var page = source.page(PAGE, cursor);
        books.addAll(page.books());
        if (page.endCursor() != null) cursor = page.endCursor();
        more = page.hasNextPage();
    }

    public void resize(int rows) { this.rows = Math.max(1, rows); scrollIntoView(); }
    public void down() throws Exception { move(1); }
    public void up() throws Exception { move(-1); }
    public void pageDown() throws Exception { move(rows); }
    public void pageUp() throws Exception { move(-rows); }
    public void home() throws Exception { move(-selected); }

    private void move(int by) throws Exception {
        int target = Math.max(0, selected + by);
        // Load until the target row exists and has LOOKAHEAD rows after it, or the feed ends.
        while (more && target + LOOKAHEAD >= books.size()) load();
        selected = Math.min(target, books.size() - 1);
        scrollIntoView();
    }

    private void scrollIntoView() {
        if (selected < top) top = selected;
        if (selected >= top + rows) top = selected - rows + 1;
    }

    public List<FeedClient.Book> books() { return books; }
    public int selected() { return selected; }
    public int top() { return top; }
    public boolean hasMore() { return more; }
}
