package gal.usc.etse.es.restdemo.controller.hal;

import gal.usc.etse.es.restdemo.controller.BenchController;
import gal.usc.etse.es.restdemo.controller.PhotoController;
import gal.usc.etse.es.restdemo.controller.UserController;
import gal.usc.etse.es.restdemo.model.dto.Photo;
import org.jspecify.annotations.NonNull;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PhotoAssembler implements RepresentationModelAssembler<Photo, EntityModel<Photo>> {
    @Override
    public @NonNull EntityModel<Photo> toModel(@NonNull Photo photo) {
        var model = EntityModel.of(photo,
                linkTo(methodOn(PhotoController.class).get(photo.photoID())).withSelfRel(),
                linkTo(methodOn(PhotoController.class).content(photo.photoID(), false)).withRel("content"),
                linkTo(methodOn(BenchController.class).get(photo.benchID())).withRel("bench"),
                linkTo(methodOn(UserController.class).get(photo.uploader())).withRel("uploader"),
                linkTo(methodOn(PhotoController.class).getByBench(photo.benchID(), null)).withRel(IanaLinkRelations.COLLECTION)
        );
        if (photo.thumbnailAvailable()) {
            model.add(linkTo(methodOn(PhotoController.class).content(photo.photoID(), true)).withRel("thumbnail"));
        }
        return model;
    }
}
