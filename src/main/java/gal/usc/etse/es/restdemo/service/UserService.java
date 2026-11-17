package gal.usc.etse.es.restdemo.service;

import gal.usc.etse.es.restdemo.exception.DuplicateUserException;
import gal.usc.etse.es.restdemo.exception.ResourceNotFoundException;
import gal.usc.etse.es.restdemo.model.dto.Bench;
import gal.usc.etse.es.restdemo.model.dto.User;
import gal.usc.etse.es.restdemo.repository.BenchRepository;
import gal.usc.etse.es.restdemo.repository.RefreshTokenRepository;
import gal.usc.etse.es.restdemo.repository.RoleRepository;
import gal.usc.etse.es.restdemo.repository.UserRepository;
import gal.usc.etse.es.utils.patch.JsonPatchOperation;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@NullMarked
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BenchRepository benchRepository;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Patcher patcher;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, BenchRepository benchRepository,
                       RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder, Patcher patcher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.benchRepository = benchRepository;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.patcher = patcher;
    }

    @Override
    @Transactional(readOnly = true)
    public gal.usc.etse.es.restdemo.model.entity.User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUserID(username).orElseThrow(() -> new UsernameNotFoundException(username));
    }

    private gal.usc.etse.es.restdemo.model.entity.User find(String userID) {
        return userRepository.findByUserID(userID).orElseThrow(() -> new ResourceNotFoundException("User", userID));
    }

    @Transactional(readOnly = true)
    public List<User> get() {
        return userRepository.findAll().stream().map(User::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<User> get(Pageable page) {
        return userRepository.findAll(page).map(User::from);
    }

    @Transactional(readOnly = true)
    public User get(String userID) {
        return User.from(find(userID));
    }

    /** Rexistro: todo usuario novo recibe só o rol USER, envíe o que envíe o cliente. */
    @Transactional
    public User create(User user) throws DuplicateUserException {
        var existing = userRepository.findByUserID(user.userID());
        if (existing.isPresent()) {
            throw new DuplicateUserException(existing.get());
        }

        var entity = new gal.usc.etse.es.restdemo.model.entity.User(
                user.userID(), user.name(), user.lastName(), user.nickname(), user.birthday(), user.location(),
                passwordEncoder.encode(user.password())
        ).addRole(roleRepository.findByRolename("USER"));

        return User.from(userRepository.save(entity));
    }

    /** PUT: substitúe os datos de perfil. O identificador e os roles non se poden cambiar por esta vía. */
    @Transactional
    public User replace(String userID, User changes) {
        var db = find(userID);
        applyProfile(db, changes);
        if (changes.password() != null && !changes.password().isBlank()) {
            db.setPassword(passwordEncoder.encode(changes.password()));
        }
        return User.from(db);
    }

    @Transactional
    public User update(String userID, List<JsonPatchOperation> changes) {
        var db = find(userID);
        User current = User.from(db);
        User patched = patcher.apply(new User(current.userID(), current.name(), current.lastName(), current.nickname(),
                current.birthday(), current.location(), null, current.roles()), changes, User.class);

        applyProfile(db, patched);
        if (patched.password() != null && !patched.password().isBlank()) {
            db.setPassword(passwordEncoder.encode(patched.password()));
        }
        return User.from(db);
    }

    private static void applyProfile(gal.usc.etse.es.restdemo.model.entity.User db, User changes) {
        db.setName(changes.name())
                .setLastName(changes.lastName())
                .setNickname(changes.nickname())
                .setBirthday(changes.birthday())
                .setLocation(changes.location());
    }

    /** Borra o usuario (as súas valoracións, comentarios e fotos bórranse en cascada) e invalida as súas sesións. */
    @Transactional
    @CacheEvict(cacheNames = BenchService.CACHE, allEntries = true)
    public void delete(String userID) {
        userRepository.delete(find(userID));
        refreshTokens.deleteAllByUser(userID);
    }

    // --- Lista de bancos gardados (favoritos) ---

    @Transactional(readOnly = true)
    public Page<Bench> getFavorites(String userID, Pageable pageable) {
        find(userID);
        return benchRepository.findFavoritesOf(userID, pageable).map(Bench::from);
    }

    @Transactional
    public void addFavorite(String userID, Long benchID) {
        find(userID).getFavoriteBenches().add(bench(benchID));
    }

    @Transactional
    public void removeFavorite(String userID, Long benchID) {
        find(userID).getFavoriteBenches().remove(bench(benchID));
    }

    // --- Likes (un por usuario e banco, polo que PUT e DELETE son idempotentes) ---

    @Transactional(readOnly = true)
    public Page<Bench> getLikes(String userID, Pageable pageable) {
        find(userID);
        return benchRepository.findLikedBy(userID, pageable).map(Bench::from);
    }

    @Transactional
    @CacheEvict(cacheNames = BenchService.CACHE, key = "#benchID")
    public void like(String userID, Long benchID) {
        find(userID).getLikedBenches().add(bench(benchID));
    }

    @Transactional
    @CacheEvict(cacheNames = BenchService.CACHE, key = "#benchID")
    public void unlike(String userID, Long benchID) {
        find(userID).getLikedBenches().remove(bench(benchID));
    }

    private gal.usc.etse.es.restdemo.model.entity.Bench bench(Long benchID) {
        return benchRepository.findById(benchID).orElseThrow(() -> new ResourceNotFoundException("Bench", benchID));
    }
}
