package gal.usc.etse.es.restdemo.event;

import gal.usc.etse.es.restdemo.service.PhotoService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Xera a miniatura dunha foto fóra do fío da petición: o cliente recibe o 201 sen agardar
 * polo reescalado da imaxe.
 */
@Component
public class PhotoThumbnailer {
    private final PhotoService photos;

    public PhotoThumbnailer(PhotoService photos) {
        this.photos = photos;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPhotoUploaded(PhotoUploadedEvent event) {
        photos.generateThumbnail(event.photoID());
    }
}
