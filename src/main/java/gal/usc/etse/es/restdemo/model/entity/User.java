package gal.usc.etse.es.restdemo.model.entity;

import jakarta.persistence.*;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@Entity
@Table(name = "users")
public class User implements UserDetails {
    // Ao borrar un banco, a base de datos elimina en cascada as filas das táboas de favoritos e likes
    private static final String BENCH_FK_CASCADE = "FOREIGN KEY (bench_id) REFERENCES benches (bench_id) ON DELETE CASCADE";

    @Id
    @Column(name = "user_id")
    private String userID;

    private String name;
    private String lastName;
    private String nickname;
    private LocalDate birthday;
    private String location;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role"))
    private Set<Role> roles = new HashSet<>();

    /** Lista de bancos gardados ("quero ir"). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_favorite_benches",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "bench_id"),
            inverseForeignKey = @ForeignKey(name = "fk_favorite_bench", foreignKeyDefinition = BENCH_FK_CASCADE))
    private Set<Bench> favoriteBenches = new HashSet<>();

    /** Bancos que lle gustan ao usuario (un like por usuario e banco). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_liked_benches",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "bench_id"),
            inverseForeignKey = @ForeignKey(name = "fk_liked_bench", foreignKeyDefinition = BENCH_FK_CASCADE))
    private Set<Bench> likedBenches = new HashSet<>();

    public User() {}

    public User(String userID, String name, String lastName, String nickname, LocalDate birthday, String location, String password) {
        this.userID = userID;
        this.name = name;
        this.lastName = lastName;
        this.nickname = nickname;
        this.birthday = birthday;
        this.location = location;
        this.password = password;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRolename()))
                .toList();
    }

    @Override
    @NonNull
    public String getPassword() { return password; }
    public User setPassword(String password) { this.password = password; return this; }

    @Override
    @NonNull
    public String getUsername() { return userID; }

    public Set<Role> getRoles() { return roles; }
    public User addRole(Role role) { this.roles.add(role); return this; }

    public Set<Bench> getFavoriteBenches() { return favoriteBenches; }
    public Set<Bench> getLikedBenches() { return likedBenches; }

    public String getUserID() { return userID; }

    public String getName() { return name; }
    public User setName(String name) { this.name = name; return this; }

    public String getLastName() { return lastName; }
    public User setLastName(String lastName) { this.lastName = lastName; return this; }

    public String getNickname() { return nickname; }
    public User setNickname(String nickname) { this.nickname = nickname; return this; }

    public LocalDate getBirthday() { return birthday; }
    public User setBirthday(LocalDate birthday) { this.birthday = birthday; return this; }

    public String getLocation() { return location; }
    public User setLocation(String location) { this.location = location; return this; }
}
