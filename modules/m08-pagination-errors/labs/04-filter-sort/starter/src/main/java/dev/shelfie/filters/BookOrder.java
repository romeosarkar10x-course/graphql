package dev.shelfie.filters;

public record BookOrder(Field field, Direction direction) {
    public enum Field { TITLE, PUBLISHED, RATING, RELEVANCE }
    public enum Direction { ASC, DESC }

    public static final BookOrder DEFAULT = new BookOrder(Field.TITLE, Direction.ASC);
}
