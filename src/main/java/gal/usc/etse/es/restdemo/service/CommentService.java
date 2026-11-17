package gal.usc.etse.es.restdemo.service;

import gal.usc.etse.es.restdemo.exception.ResourceNotFoundException;
import gal.usc.etse.es.restdemo.model.dto.Comment;
import gal.usc.etse.es.restdemo.repository.BenchRepository;
import gal.usc.etse.es.restdemo.repository.CommentRepository;
import gal.usc.etse.es.restdemo.repository.UserRepository;
import gal.usc.etse.es.utils.patch.JsonPatchOperation;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@NullMarked
public class CommentService {
    private final CommentRepository comments;
    private final BenchRepository benches;
    private final UserRepository users;
    private final Patcher patcher;

    public CommentService(CommentRepository comments, BenchRepository benches, UserRepository users, Patcher patcher) {
        this.comments = comments;
        this.benches = benches;
        this.users = users;
        this.patcher = patcher;
    }

    @Transactional(readOnly = true)
    public Page<Comment> getByBench(Long benchID, Pageable pageable) {
        if (!benches.existsById(benchID)) throw new ResourceNotFoundException("Bench", benchID);
        return comments.findByBench_BenchID(benchID, pageable).map(Comment::from);
    }

    @Transactional(readOnly = true)
    public Comment get(Long id) {
        return comments.findById(id).map(Comment::from).orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }

    @Transactional
    public Comment create(Long benchID, String author, Comment comment) {
        var bench = benches.findById(benchID).orElseThrow(() -> new ResourceNotFoundException("Bench", benchID));
        var entity = new gal.usc.etse.es.restdemo.model.entity.Comment(users.getReferenceById(author), bench, comment.text().trim());
        return Comment.from(comments.save(entity));
    }

    @Transactional
    public Comment update(Long id, List<JsonPatchOperation> changes) {
        var entity = comments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment", id));
        Comment patched = patcher.apply(Comment.from(entity), changes, Comment.class);
        if (!Objects.equals(patched.text(), entity.getText())) {
            entity.setText(patched.text().trim());
        }
        return Comment.from(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!comments.existsById(id)) throw new ResourceNotFoundException("Comment", id);
        comments.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean canModify(Long id, String user) {
        return comments.findById(id).map(c -> c.getAuthor().getUserID().equals(user)).orElse(true);
    }
}
