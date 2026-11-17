INSERT INTO comments(comment_id, author_id, bench_id, text, created_at) VALUES
(1, 'ana',   1,  'Alguén sabe se o pintaron xa?',                          '2026-10-10 10:00:00+02'),
(2, 'user',  1,  'Si, a semana pasada. Quedou moi ben.',                   '2026-10-10 12:30:00+02'),
(3, 'brais', 4,  'Hai que ir cedo, enchese de turistas.',                  '2026-10-11 09:00:00+02'),
(4, 'admin', 9,  'Lembrade que o porche pecha ás 21:00.',                  '2026-10-12 18:00:00+02'),
(5, 'user',  14, 'Paso obrigado para os peregrinos.',                      '2026-10-13 11:00:00+02'),
(6, 'ana',   14, 'O amencer dende aquí paga a pena.',                      '2026-10-13 13:00:00+02'),
(7, 'brais', 20, 'Ten unha táboa solta, coidado.',                         '2026-10-14 17:00:00+02'),
(8, 'user',  10, 'Hai moitos mosquitos no verán.',                         '2026-10-15 20:00:00+02');

SELECT setval(pg_get_serial_sequence('comments', 'comment_id'), (SELECT MAX(comment_id) FROM comments));
