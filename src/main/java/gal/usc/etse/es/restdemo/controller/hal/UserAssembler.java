package gal.usc.etse.es.restdemo.controller.hal;

import gal.usc.etse.es.restdemo.controller.ReviewController;
import gal.usc.etse.es.restdemo.controller.UserController;
import gal.usc.etse.es.restdemo.model.dto.User;
import org.jspecify.annotations.NonNull;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class UserAssembler implements RepresentationModelAssembler<User, EntityModel<User>> {
    @Override
    public @NonNull EntityModel<User> toModel(@NonNull User user) {
        String id = user.userID();
        return EntityModel.of(user,
                linkTo(methodOn(UserController.class).get(id)).withSelfRel(),
                linkTo(methodOn(ReviewController.class).getByAuthor(id, null)).withRel("reviews"),
                linkTo(methodOn(UserController.class).getFavorites(id, null)).withRel("favorites"),
                linkTo(methodOn(UserController.class).getLikes(id, null)).withRel("likes")
        );
    }
}
