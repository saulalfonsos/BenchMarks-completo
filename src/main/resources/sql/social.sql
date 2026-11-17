-- Lista de bancos gardados (favoritos) e likes
INSERT INTO user_favorite_benches(user_id, bench_id) VALUES
('user', 4), ('user', 14), ('user', 20),
('ana', 1), ('ana', 6),
('brais', 19);

INSERT INTO user_liked_benches(user_id, bench_id) VALUES
('user', 1), ('user', 4), ('user', 10), ('user', 14),
('ana', 1), ('ana', 2), ('ana', 14), ('ana', 19),
('brais', 1), ('brais', 4), ('brais', 20),
('admin', 14), ('admin', 1);
