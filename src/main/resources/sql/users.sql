-- Contrasinal de todos os usuarios de proba: 1234 (gardado con bcrypt)
INSERT INTO users(user_id, name, last_name, nickname, birthday, location, password) VALUES
('user',  'Regular', 'User',     'user',  '1995-01-01', 'Santiago de Compostela', '{bcrypt}$2y$10$ZPwxWDZvjBYJQ6oEv9IHFudbWsDcYv51ESR/aeKTM3bQLONrt4ZKO'),
('admin', 'Admin',   'User',     'admin', '1990-01-01', 'A Coruña',               '{bcrypt}$2y$10$ZPwxWDZvjBYJQ6oEv9IHFudbWsDcYv51ESR/aeKTM3bQLONrt4ZKO'),
('ana',   'Ana',     'Varela',   'anav',  '1999-05-12', 'Santiago de Compostela', '{bcrypt}$2y$10$ZPwxWDZvjBYJQ6oEv9IHFudbWsDcYv51ESR/aeKTM3bQLONrt4ZKO'),
('brais', 'Brais',   'Lorenzo',  'brais', '2001-09-30', 'Lugo',                   '{bcrypt}$2y$10$ZPwxWDZvjBYJQ6oEv9IHFudbWsDcYv51ESR/aeKTM3bQLONrt4ZKO');

INSERT INTO user_roles(user_id, role) VALUES
('user', 'USER'),
('admin', 'ADMIN'),
('ana', 'USER'),
('brais', 'USER');
