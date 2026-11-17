package gal.usc.etse.es.restdemo.repository;

import gal.usc.etse.es.restdemo.model.entity.Comment;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@NullMarked
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByBench_BenchID(Long benchID, Pageable pageable);
}
