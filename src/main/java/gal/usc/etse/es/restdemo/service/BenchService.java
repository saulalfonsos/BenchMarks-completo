package gal.usc.etse.es.restdemo.service;

import gal.usc.etse.es.restdemo.exception.ResourceNotFoundException;
import gal.usc.etse.es.restdemo.model.dto.Bench;
import gal.usc.etse.es.restdemo.repository.BenchRepository;
import gal.usc.etse.es.restdemo.repository.UserRepository;
import gal.usc.etse.es.utils.patch.JsonPatchOperation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static gal.usc.etse.es.restdemo.repository.BenchSpecifications.*;

@Service
@NullMarked
public class BenchService {
    public static final String CACHE = "benches";

    private final BenchRepository benches;
    private final UserRepository users;
    private final Patcher patcher;

    public BenchService(BenchRepository benches, UserRepository users, Patcher patcher) {
        this.benches = benches;
        this.users = users;
        this.patcher = patcher;
    }

    /** Filtros de busca. Todos son opcionais e combínanse con AND. */
    public record Filter(
            @Nullable String q,
            @Nullable String type,
            @Nullable Boolean covered,
            @Nullable Double minMark,
            @Nullable String owner,
            @Nullable Double latitude,
            @Nullable Double longitude,
            @Nullable Double radiusKm
    ) {
        public Filter {
            boolean anyGeo = latitude != null || longitude != null || radiusKm != null;
            boolean allGeo = latitude != null && longitude != null && radiusKm != null;
            if (anyGeo && !allGeo) {
                throw new IllegalArgumentException("Geographic search requires lat, lon and radius together");
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<Bench> search(Filter filter, Pageable pageable) {
        var spec = Specification.allOf(
                matches(filter.q()),
                hasType(filter.type()),
                isCovered(filter.covered()),
                minAverageMark(filter.minMark()),
                ownedBy(filter.owner()),
                near(filter.latitude(), filter.longitude(), filter.radiusKm())
        );
        return benches.findAll(spec, pageable).map(Bench::from);
    }

    @Cacheable(cacheNames = CACHE, key = "#id")
    @Transactional(readOnly = true)
    public Bench get(Long id) {
        return benches.findById(id).map(Bench::from).orElseThrow(() -> new ResourceNotFoundException("Bench", id));
    }

    @Transactional
    public Bench create(Bench bench, String owner) {
        var entity = new gal.usc.etse.es.restdemo.model.entity.Bench()
                .setName(bench.name())
                .setDescription(bench.description())
                .setLatitude(bench.latitude())
                .setLongitude(bench.longitude())
                .setType(bench.type())
                .setCovered(Boolean.TRUE.equals(bench.covered()))
                .setOwner(users.getReferenceById(owner));

        return Bench.from(benches.save(entity));
    }

    /** Só se poden modificar os campos editables; os calculados polo servidor ignóranse. */
    @CacheEvict(cacheNames = CACHE, key = "#id")
    @Transactional
    public Bench update(Long id, List<JsonPatchOperation> changes) {
        var entity = benches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bench", id));
        Bench patched = patcher.apply(Bench.from(entity), changes, Bench.class);

        entity.setName(patched.name())
                .setDescription(patched.description())
                .setLatitude(patched.latitude())
                .setLongitude(patched.longitude())
                .setType(patched.type())
                .setCovered(Boolean.TRUE.equals(patched.covered()));

        return Bench.from(entity);
    }

    @CacheEvict(cacheNames = CACHE, key = "#id")
    @Transactional
    public void delete(Long id) {
        if (!benches.existsById(id)) throw new ResourceNotFoundException("Bench", id);
        benches.deleteById(id);
    }

    /**
     * Regra de propiedade usada nas expresións @PreAuthorize: o creador do banco pode modificalo.
     * Se o banco non existe devólvese true para que a petición continúe e responda 404 en lugar de 403.
     */
    @Transactional(readOnly = true)
    public boolean canModify(Long id, String user) {
        return benches.findById(id)
                .map(b -> b.getOwner() != null && b.getOwner().getUserID().equals(user))
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public void ensureExists(Long id) {
        if (!benches.existsById(id)) throw new ResourceNotFoundException("Bench", id);
    }
}
