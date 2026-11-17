package gal.usc.etse.es.restdemo.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Foto dun banco. Os bytes da imaxe e da miniatura gárdanse na propia base de datos (bytea);
 * os listados usan proxeccións (PhotoRepository) para non cargar nunca o binario.
 */
@Entity
@Table(name = "photos")
@SuppressWarnings("unused")
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long photoID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bench_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Bench bench;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User uploader;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private long size;

    private int width;
    private int height;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private byte[] data;

    private byte[] thumbnail;

    public Photo() {}

    public Photo(Bench bench, User uploader, String contentType, byte[] data, int width, int height) {
        this.bench = bench;
        this.uploader = uploader;
        this.contentType = contentType;
        this.data = data;
        this.size = data.length;
        this.width = width;
        this.height = height;
        this.uploadedAt = Instant.now();
    }

    public Long getPhotoID() { return photoID; }
    public Bench getBench() { return bench; }
    public User getUploader() { return uploader; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Instant getUploadedAt() { return uploadedAt; }
    public byte[] getData() { return data; }

    public byte[] getThumbnail() { return thumbnail; }
    public Photo setThumbnail(byte[] thumbnail) { this.thumbnail = thumbnail; return this; }
}
