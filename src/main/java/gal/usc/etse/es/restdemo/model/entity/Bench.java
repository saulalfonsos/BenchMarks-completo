package gal.usc.etse.es.restdemo.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "benches")
@SuppressWarnings("unused")
public class Bench {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bench_id")
    private Long benchID;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 50)
    private String type;

    /** O banco está baixo unha cuberta (marquesiña, árbore, soportal...) e polo tanto sempre á sombra. */
    @Column(nullable = false)
    private boolean covered = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User owner;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Valores desnormalizados que recalcula de forma asíncrona o RatingUpdater cando cambian as valoracións. */
    @Column(name = "average_mark")
    private Double averageMark;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Formula("(SELECT count(*) FROM user_liked_benches l WHERE l.bench_id = bench_id)")
    private long likes;

    public Bench() {}

    public Long getBenchID() { return benchID; }
    public Bench setBenchID(Long benchID) { this.benchID = benchID; return this; }

    public String getName() { return name; }
    public Bench setName(String name) { this.name = name; return this; }

    public String getDescription() { return description; }
    public Bench setDescription(String description) { this.description = description; return this; }

    public Double getLatitude() { return latitude; }
    public Bench setLatitude(Double latitude) { this.latitude = latitude; return this; }

    public Double getLongitude() { return longitude; }
    public Bench setLongitude(Double longitude) { this.longitude = longitude; return this; }

    public String getType() { return type; }
    public Bench setType(String type) { this.type = type; return this; }

    public boolean isCovered() { return covered; }
    public Bench setCovered(boolean covered) { this.covered = covered; return this; }

    public User getOwner() { return owner; }
    public Bench setOwner(User owner) { this.owner = owner; return this; }

    public Instant getCreatedAt() { return createdAt; }
    public Bench setCreatedAt(Instant createdAt) { this.createdAt = createdAt; return this; }

    public Double getAverageMark() { return averageMark; }
    public Bench setAverageMark(Double averageMark) { this.averageMark = averageMark; return this; }

    public int getReviewCount() { return reviewCount; }
    public Bench setReviewCount(int reviewCount) { this.reviewCount = reviewCount; return this; }

    public long getLikes() { return likes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bench bench)) return false;
        return benchID != null && Objects.equals(benchID, bench.benchID);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(benchID);
    }
}
