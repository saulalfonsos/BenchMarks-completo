package gal.usc.etse.es.restdemo.controller.hal;

import org.springframework.data.domain.Page;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Converte unha páxina de Spring Data nun PagedModel HAL: _embedded cos recursos (cada un coas súas ligazóns),
 * metadatos de paxinación e ligazóns self/first/prev/next/last construídas a partir da petición actual.
 */
@Component
public class Pages {
    private final HateoasPageableHandlerMethodArgumentResolver pageableResolver;

    public Pages(HateoasPageableHandlerMethodArgumentResolver pageableResolver) {
        this.pageableResolver = pageableResolver;
    }

    @SuppressWarnings("unchecked")
    public <T> PagedModel<EntityModel<T>> toModel(Page<T> page, Class<T> type, RepresentationModelAssembler<T, EntityModel<T>> assembler) {
        var pagedAssembler = new PagedResourcesAssembler<T>(pageableResolver, null);
        if (page.isEmpty()) {
            return (PagedModel<EntityModel<T>>) (PagedModel<?>) pagedAssembler.toEmptyModel(page, type);
        }
        return pagedAssembler.toModel(page, assembler);
    }
}
