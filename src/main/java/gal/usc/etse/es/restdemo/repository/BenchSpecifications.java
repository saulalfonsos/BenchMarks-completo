package gal.usc.etse.es.restdemo.repository;

import gal.usc.etse.es.restdemo.model.entity.Bench;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criterios de busca de bancos. Combínanse dinamicamente segundo os parámetros que chegan na petición,
 * de xeito que se constrúe unha única consulta SQL paxinada.
 */
public final class BenchSpecifications {
    private static final double KM_PER_DEGREE = 111.32;

    private BenchSpecifications() {}

    /** Texto libre sobre nome, descrición e tipo (sen distinguir maiúsculas). */
    public static Specification<Bench> matches(@Nullable String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) return null;
            String pattern = "%" + text.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("type")), pattern)
            );
        };
    }

    public static Specification<Bench> hasType(@Nullable String type) {
        return (root, query, cb) -> type == null || type.isBlank() ? null : cb.equal(cb.lower(root.get("type")), type.trim().toLowerCase());
    }

    public static Specification<Bench> isCovered(@Nullable Boolean covered) {
        return (root, query, cb) -> covered == null ? null : cb.equal(root.get("covered"), covered);
    }

    public static Specification<Bench> minAverageMark(@Nullable Double minMark) {
        return (root, query, cb) -> minMark == null ? null : cb.greaterThanOrEqualTo(root.get("averageMark"), minMark);
    }

    public static Specification<Bench> ownedBy(@Nullable String owner) {
        return (root, query, cb) -> owner == null || owner.isBlank() ? null : cb.equal(root.get("owner").get("userID"), owner);
    }

    /**
     * Bancos dentro dun radio (km) arredor dun punto. Úsase unha caixa de coordenadas
     * (aproximación válida para radios pequenos) que a base de datos pode resolver con índices.
     */
    public static Specification<Bench> near(@Nullable Double latitude, @Nullable Double longitude, @Nullable Double radiusKm) {
        return (root, query, cb) -> {
            if (latitude == null || longitude == null || radiusKm == null) return null;
            double dLat = radiusKm / KM_PER_DEGREE;
            double dLon = radiusKm / (KM_PER_DEGREE * Math.max(Math.cos(Math.toRadians(latitude)), 0.01));
            return cb.and(
                    cb.between(root.get("latitude"), latitude - dLat, latitude + dLat),
                    cb.between(root.get("longitude"), longitude - dLon, longitude + dLon)
            );
        };
    }
}
