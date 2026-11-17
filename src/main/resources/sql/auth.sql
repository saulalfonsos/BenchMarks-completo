-- Permisos por recurso e acción. Os roles herdan permisos (ADMIN inclúe USER).
-- Ademais dos permisos, as operacións de modificación permiten ao propietario do recurso (ver @PreAuthorize).
INSERT INTO permissions(id, resource, action) VALUES
('users:read', 'users', 'read'),
('users:update', 'users', 'update'),
('users:delete', 'users', 'delete'),
('benches:read', 'benches', 'read'),
('benches:create', 'benches', 'create'),
('benches:update', 'benches', 'update'),
('benches:delete', 'benches', 'delete'),
('reviews:read', 'reviews', 'read'),
('reviews:create', 'reviews', 'create'),
('reviews:update', 'reviews', 'update'),
('reviews:delete', 'reviews', 'delete'),
('comments:read', 'comments', 'read'),
('comments:create', 'comments', 'create'),
('comments:update', 'comments', 'update'),
('comments:delete', 'comments', 'delete'),
('photos:read', 'photos', 'read'),
('photos:create', 'photos', 'create'),
('photos:delete', 'photos', 'delete');

INSERT INTO roles(rolename) VALUES
('ADMIN'),
('USER');

INSERT INTO role_hierarchy(role, includes) VALUES
('ADMIN', 'USER');

INSERT INTO role_permissions(role, permission) VALUES
('USER', 'users:read'),
('USER', 'benches:read'),
('USER', 'benches:create'),
('USER', 'reviews:read'),
('USER', 'reviews:create'),
('USER', 'comments:read'),
('USER', 'comments:create'),
('USER', 'photos:read'),
('USER', 'photos:create'),
('ADMIN', 'users:update'),
('ADMIN', 'users:delete'),
('ADMIN', 'benches:update'),
('ADMIN', 'benches:delete'),
('ADMIN', 'reviews:update'),
('ADMIN', 'reviews:delete'),
('ADMIN', 'comments:update'),
('ADMIN', 'comments:delete'),
('ADMIN', 'photos:delete');
