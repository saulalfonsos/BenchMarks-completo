package gal.usc.etse.es.restdemo.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "comments")
@SuppressWarnings("unused")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bench_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Bench bench;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    public Comment() {}

    public Comment(User author, Bench bench, String text) {
        this.author = author;
        this.bench = bench;
        this.text = text;
        this.createdAt = Instant.now();
    }

    public Long getCommentID() { return commentID; }
    public User getAuthor() { return author; }
    public Bench getBench() { return bench; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEditedAt() { return editedAt; }

    public String getText() { return text; }
    public Comment setText(String text) {
        this.text = text;
        this.editedAt = Instant.now();
        return this;
    }
}
