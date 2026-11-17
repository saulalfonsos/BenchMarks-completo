package gal.usc.etse.es.restdemo.exception;

/** O usuario xa valorou ese banco. Tradúcese a 409 con Location apuntando á valoración existente. */
public class DuplicateReviewException extends RuntimeException {
    private final String author;
    private final Long benchID;
    private final Long existingReviewID;

    public DuplicateReviewException(String author, Long benchID, Long existingReviewID) {
        super("User " + author + " already reviewed bench " + benchID);
        this.author = author;
        this.benchID = benchID;
        this.existingReviewID = existingReviewID;
    }

    public String getAuthor() { return author; }
    public Long getBenchID() { return benchID; }
    public Long getExistingReviewID() { return existingReviewID; }
}
