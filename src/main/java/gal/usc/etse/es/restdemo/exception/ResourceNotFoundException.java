package gal.usc.etse.es.restdemo.exception;

/** Recurso inexistente (banco, valoración, comentario, foto ou usuario). Tradúcese a 404. */
public class ResourceNotFoundException extends RuntimeException {
    private final String resource;
    private final Object id;

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " not found");
        this.resource = resource;
        this.id = id;
    }

    public String getResource() { return resource; }
    public Object getId() { return id; }
}
