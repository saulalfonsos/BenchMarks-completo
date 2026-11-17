package gal.usc.etse.es.restdemo.event;

/** Publícase tras gardar unha foto nova, para xerar a súa miniatura en segundo plano. */
public record PhotoUploadedEvent(Long photoID) {}
