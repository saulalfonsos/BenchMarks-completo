package gal.usc.etse.es.restdemo.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_author_bench", columnNames = {"author_id", "bench_id"}))
@SuppressWarnings("unused")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bench_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Bench bench;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer mark;

    @Column(length = 2000)
    private String note;

    public Review() {}

    public Review(User author, Bench bench, LocalDate date, Integer mark, String note) {
        this.author = author;
        this.bench = bench;
        this.date = date;
        this.mark = mark;
        this.note = note;
    }

    public Long getReviewID() { return reviewID; }
    public User getAuthor() { return author; }
    public Bench getBench() { return bench; }

    public LocalDate getDate() { return date; }
    public Review setDate(LocalDate date) { this.date = date; return this; }

    public Integer getMark() { return mark; }
    public Review setMark(Integer mark) { this.mark = mark; return this; }

    public String getNote() { return note; }
    public Review setNote(String note) { this.note = note; return this; }
}
