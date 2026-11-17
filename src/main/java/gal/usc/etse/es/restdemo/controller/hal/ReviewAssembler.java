package gal.usc.etse.es.restdemo.controller.hal;

import gal.usc.etse.es.restdemo.controller.BenchController;
import gal.usc.etse.es.restdemo.controller.ReviewController;
import gal.usc.etse.es.restdemo.controller.UserController;
import gal.usc.etse.es.restdemo.model.dto.Review;
import org.jspecify.annotations.NonNull;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ReviewAssembler implements RepresentationModelAssembler<Review, EntityModel<Review>> {
    @Override
    public @NonNull EntityModel<Review> toModel(@NonNull Review review) {
        return EntityModel.of(review,
                linkTo(methodOn(ReviewController.class).get(review.reviewID())).withSelfRel(),
                linkTo(methodOn(BenchController.class).get(review.benchID())).withRel("bench"),
                linkTo(methodOn(UserController.class).get(review.author())).withRel(IanaLinkRelations.AUTHOR),
                linkTo(methodOn(ReviewController.class).getByBench(review.benchID(), null)).withRel(IanaLinkRelations.COLLECTION)
        );
    }
}
