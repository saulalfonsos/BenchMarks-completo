package gal.usc.etse.es.restdemo.repository;

import gal.usc.etse.es.restdemo.model.entity.RefreshToken;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnusedReturnValue")
@NullMarked
@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    // Os repositorios de Redis non executan consultas derivadas de borrado: búscase e bórrase en dous pasos
    List<RefreshToken> findAllByUser(String user);

    default void deleteAllByUser(String user) {
        deleteAll(findAllByUser(user));
    }
    Optional<RefreshToken> findByToken(String token);
}
