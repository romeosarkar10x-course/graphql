package dev.shelfie.tui;

import java.util.ArrayList;
import java.util.List;

/**
 * The browser's state, without any terminal: the books loaded so far, the selection, and the first
 * visible row. TODO (mini-project 8): load more pages by cursor as the user moves down, page keys, Home,
 * and keep the selection on screen.
 */
public final class Browser {
    public static final int PAGE = 20;

    public interface Source { FeedClient.Page page(int first, String after) throws Exception; }

    private final Source source;
    private final List<FeedClient.Book> books = new ArrayList<>();
    private boolean more;
    private int selected, top, rows;

    public Browser(Source source, int rows) throws Exception {
        this.source = source;
        this.rows = rows;
        var first = source.page(PAGE, null);
        books.addAll(first.books());
        more = first.hasNextPage();
    }

    public void resize(int rows) { this.rows = Math.max(1, rows); }
    public void down() throws Exception { selected = Math.min(selected + 1, books.size() - 1); }
    public void up() throws Exception { selected = Math.max(selected - 1, 0); }
    public void pageDown() throws Exception { }
    public void pageUp() throws Exception { }
    public void home() throws Exception { }

    public List<FeedClient.Book> books() { return books; }
    public int selected() { return selected; }
    public int top() { return top; }
    public boolean hasMore() { return more; }
}
