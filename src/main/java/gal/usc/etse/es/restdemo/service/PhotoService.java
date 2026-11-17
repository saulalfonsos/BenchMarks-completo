package gal.usc.etse.es.restdemo.service;

import gal.usc.etse.es.restdemo.event.PhotoUploadedEvent;
import gal.usc.etse.es.restdemo.exception.InvalidPhotoException;
import gal.usc.etse.es.restdemo.exception.ResourceNotFoundException;
import gal.usc.etse.es.restdemo.model.dto.Photo;
import gal.usc.etse.es.restdemo.repository.BenchRepository;
import gal.usc.etse.es.restdemo.repository.PhotoRepository;
import gal.usc.etse.es.restdemo.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
@NullMarked
public class PhotoService {
    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);
    private static final int THUMBNAIL_WIDTH = 320;
    /** Límite de píxeles: evita que unha imaxe pequena comprimida declare dimensións enormes e esgote a memoria ao decodificala. */
    private static final long MAX_PIXELS = 40_000_000L;
    private static final int MAX_SIDE = 12_000;

    private final PhotoRepository photos;
    private final BenchRepository benches;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public PhotoService(PhotoRepository photos, BenchRepository benches, UserRepository users, ApplicationEventPublisher events) {
        this.photos = photos;
        this.benches = benches;
        this.users = users;
        this.events = events;
    }

    /**
     * Binario dunha foto xunto co seu tipo MIME.
     * @param provisional pediuse a miniatura pero aínda non está xerada e devólvese a orixinal (non debe cachearse)
     */
    public record Content(byte[] bytes, MediaType mediaType, boolean provisional) {}

    /**
     * Garda unha foto para un banco. O tipo detéctase polos bytes (non se confía na cabeceira do cliente)
     * e só se aceptan JPEG e PNG. A miniatura xérase despois, de forma asíncrona.
     */
    @Transactional
    public Photo upload(Long benchID, String uploader, byte[] bytes) {
        var bench = benches.findById(benchID).orElseThrow(() -> new ResourceNotFoundException("Bench", benchID));
        MediaType type = detectType(bytes);
        BufferedImage image = decode(bytes);

        var saved = photos.save(new gal.usc.etse.es.restdemo.model.entity.Photo(
                bench, users.getReferenceById(uploader), type.toString(), bytes, image.getWidth(), image.getHeight()
        ));
        events.publishEvent(new PhotoUploadedEvent(saved.getPhotoID()));

        return new Photo(saved.getPhotoID(), benchID, uploader, saved.getContentType(), saved.getSize(),
                saved.getWidth(), saved.getHeight(), saved.getUploadedAt(), false);
    }

    @Transactional(readOnly = true)
    public Page<Photo> getByBench(Long benchID, Pageable pageable) {
        if (!benches.existsById(benchID)) throw new ResourceNotFoundException("Bench", benchID);
        return photos.findMetadataByBench(benchID, pageable);
    }

    @Transactional(readOnly = true)
    public Photo get(Long id) {
        return photos.findMetadata(id).orElseThrow(() -> new ResourceNotFoundException("Photo", id));
    }

    /** Devolve a miniatura se se pide e xa está xerada; noutro caso, a imaxe orixinal. */
    @Transactional(readOnly = true)
    public Content getContent(Long id, boolean thumbnail) {
        var photo = photos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Photo", id));
        if (thumbnail && photo.getThumbnail() != null) {
            return new Content(photo.getThumbnail(), MediaType.IMAGE_JPEG, false);
        }
        return new Content(photo.getData(), MediaType.parseMediaType(photo.getContentType()), thumbnail);
    }

    @Transactional
    public void delete(Long id) {
        if (!photos.existsById(id)) throw new ResourceNotFoundException("Photo", id);
        photos.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean canModify(Long id, String user) {
        return photos.findUploader(id).map(user::equals).orElse(true);
    }

    /** Xera e garda a miniatura (JPEG de 320 px de ancho). Chámase dende PhotoThumbnailer en segundo plano. */
    @Transactional
    public void generateThumbnail(Long id) {
        photos.findById(id).ifPresent(photo -> {
            BufferedImage source = decode(photo.getData());
            int height = Math.max(1, source.getHeight() * THUMBNAIL_WIDTH / source.getWidth());
            BufferedImage scaled = new BufferedImage(THUMBNAIL_WIDTH, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE); // fondo para PNG con transparencia
            g.fillRect(0, 0, THUMBNAIL_WIDTH, height);
            g.drawImage(source, 0, 0, THUMBNAIL_WIDTH, height, null);
            g.dispose();

            try (var out = new ByteArrayOutputStream()) {
                ImageIO.write(scaled, "jpg", out);
                photo.setThumbnail(out.toByteArray());
                log.debug("Thumbnail generated for photo {}", id);
            } catch (IOException e) {
                log.warn("Could not generate thumbnail for photo {}", id, e);
            }
        });
    }

    private static MediaType detectType(byte[] bytes) {
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG;
        }
        if (bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        throw new InvalidPhotoException("Only JPEG and PNG images are accepted");
    }

    private static BufferedImage decode(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new InvalidPhotoException("The file is not a readable image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                // Só se le a cabeceira: compróbanse as dimensións antes de reservar memoria para os píxeles
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width > MAX_SIDE || height > MAX_SIDE || (long) width * height > MAX_PIXELS) {
                    throw new InvalidPhotoException("Image too large: " + width + "x" + height + " (maximum 40 megapixels)");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidPhotoException("The file is not a readable image");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw new InvalidPhotoException("The file is not a readable image");
            return image;
        } catch (IOException e) {
            throw new InvalidPhotoException("The file is not a readable image");
        }
    }
}
