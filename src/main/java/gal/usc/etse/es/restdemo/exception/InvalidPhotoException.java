package gal.usc.etse.es.restdemo.exception;

/** O ficheiro subido non é unha imaxe JPEG/PNG válida. Tradúcese a 415. */
public class InvalidPhotoException extends RuntimeException {
    public InvalidPhotoException(String message) {
        super(message);
    }
}
