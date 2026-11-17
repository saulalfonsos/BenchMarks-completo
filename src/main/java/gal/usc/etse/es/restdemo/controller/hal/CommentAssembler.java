package gal.usc.etse.es.restdemo.controller.hal;

import gal.usc.etse.es.restdemo.controller.BenchController;
import gal.usc.etse.es.restdemo.controller.CommentController;
import gal.usc.etse.es.restdemo.controller.UserController;
import gal.usc.etse.es.restdemo.model.dto.Comment;
import org.jspecify.annotations.NonNull;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class CommentAssembler implements RepresentationModelAssembler<Comment, EntityModel<Comment>> {
    @Override
    public @NonNull EntityModel<Comment> toModel(@NonNull Comment comment) {
        return EntityModel.of(comment,
                linkTo(methodOn(CommentController.class).get(comment.commentID())).withSelfRel(),
                linkTo(methodOn(BenchController.class).get(comment.benchID())).withRel("bench"),
                linkTo(methodOn(UserController.class).get(comment.author())).withRel(IanaLinkRelations.AUTHOR),
                linkTo(methodOn(CommentController.class).getByBench(comment.benchID(), null)).withRel(IanaLinkRelations.COLLECTION)
        );
    }
}
