package gal.usc.etse.es.restdemo.controller.hal;

import gal.usc.etse.es.restdemo.controller.*;
import gal.usc.etse.es.restdemo.model.dto.Bench;
import org.jspecify.annotations.NonNull;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class BenchAssembler implements RepresentationModelAssembler<Bench, EntityModel<Bench>> {
    @Override
    public @NonNull EntityModel<Bench> toModel(@NonNull Bench bench) {
        Long id = bench.benchID();
        var model = EntityModel.of(bench,
                linkTo(methodOn(BenchController.class).get(id)).withSelfRel(),
                linkTo(methodOn(ReviewController.class).getByBench(id, null)).withRel("reviews"),
                linkTo(methodOn(CommentController.class).getByBench(id, null)).withRel("comments"),
                linkTo(methodOn(PhotoController.class).getByBench(id, null)).withRel("photos"),
                linkTo(methodOn(BenchController.class).shade(id, null, null, null)).withRel("shade"),
                linkTo(BenchController.class).withRel(IanaLinkRelations.COLLECTION)
        );
        if (bench.owner() != null) {
            model.add(linkTo(methodOn(UserController.class).get(bench.owner())).withRel("owner"));
        }
        return model;
    }
}
