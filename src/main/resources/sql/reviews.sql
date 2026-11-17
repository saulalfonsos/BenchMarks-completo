INSERT INTO reviews(review_id, author_id, bench_id, date, mark, note) VALUES
(1,  'user',  1,  '2026-10-01', 9,  'Vistas espectaculares ao solpor.'),
(2,  'ana',   1,  '2026-10-02', 10, 'O mellor banco de Santiago.'),
(3,  'brais', 1,  '2026-10-03', 8,  'Moita xente os domingos.'),
(4,  'admin', 2,  '2026-10-04', 7,  'Bonito pero algo húmido.'),
(5,  'user',  2,  '2026-10-05', 8,  'Moi tranquilo.'),
(6,  'ana',   3,  '2026-10-06', 6,  'Duro de máis, é pedra.'),
(7,  'brais', 4,  '2026-10-07', 9,  'Sombra perfecta no verán.'),
(8,  'user',  4,  '2026-10-08', 8,  'Ideal para ler.'),
(9,  'admin', 5,  '2026-10-09', 7,  'Vistas boas, banco mellorable.'),
(10, 'ana',   6,  '2026-10-10', 9,  'Mirador precioso.'),
(11, 'user',  7,  '2026-10-11', 5,  'Frío no inverno, é metálico.'),
(12, 'brais', 8,  '2026-10-12', 7,  'Ben para descansar entre clases.'),
(13, 'admin', 9,  '2026-10-13', 6,  'Útil cando chove, pouco máis.'),
(14, 'user',  10, '2026-10-14', 8,  'O son do río é relaxante.'),
(15, 'ana',   11, '2026-10-15', 7,  'Pequeno pero salva dunha treboada.'),
(16, 'brais', 12, '2026-10-16', 6,  'Algo afastado.'),
(17, 'user',  13, '2026-10-17', 4,  'Demasiado ruído.'),
(18, 'admin', 14, '2026-10-18', 10, 'Emoción pura ao ver a Catedral.'),
(19, 'ana',   15, '2026-10-19', 8,  'Moi agradable.'),
(20, 'brais', 16, '2026-10-20', 3,  'É unha parada de autobús...'),
(21, 'user',  17, '2026-10-21', 8,  'Clásico.'),
(22, 'admin', 18, '2026-10-22', 6,  'Correcto.'),
(23, 'ana',   19, '2026-10-23', 9,  'Paz absoluta.'),
(24, 'user',  20, '2026-10-24', 9,  'Unha xoia escondida.'),
(25, 'brais', 20, '2026-10-25', 8,  'Ben, pero custa chegar.');

SELECT setval(pg_get_serial_sequence('reviews', 'review_id'), (SELECT MAX(review_id) FROM reviews));

-- Valores desnormalizados (a aplicación mantenos actualizados mediante eventos)
UPDATE benches b SET
    average_mark = (SELECT avg(r.mark) FROM reviews r WHERE r.bench_id = b.bench_id),
    review_count = (SELECT count(*) FROM reviews r WHERE r.bench_id = b.bench_id);
