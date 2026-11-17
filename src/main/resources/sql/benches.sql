INSERT INTO benches(bench_id, name, description, latitude, longitude, type, covered, owner_id, created_at, review_count) VALUES
(1,  'Paseo da Ferradura',          'Na Alameda, coa mellor vista da Catedral ao solpor.',                 42.8783, -8.5497, 'Stone',    false, 'user',  '2026-09-20 10:00:00+02', 0),
(2,  'Parque de Bonaval',           'Banco de madeira entre os bancais do antigo convento.',                42.8834, -8.5394, 'Wooden',   false, 'ana',   '2026-09-21 11:00:00+02', 0),
(3,  'Praza da Quintana',           'Escaleira de pedra onde senta media cidade.',                          42.8807, -8.5437, 'Stone',    false, 'admin', '2026-09-22 12:00:00+02', 0),
(4,  'Carballeira de Santa Susana', 'Baixo os carballos: sombra garantida todo o día.',                     42.8780, -8.5470, 'Wooden',   true,  'user',  '2026-09-23 09:30:00+02', 0),
(5,  'Parque de Belvís',            'Tranquilo, con vistas ao casco histórico.',                            42.8782, -8.5375, 'Wooden',   false, 'brais', '2026-09-24 16:00:00+02', 0),
(6,  'Mirador de Bonaval',          'No alto do parque, ideal para ler.',                                   42.8838, -8.5389, 'Stone',    false, 'ana',   '2026-09-25 17:00:00+02', 0),
(7,  'Praza de Cervantes',          'Banco metálico xunto á fonte.',                                        42.8815, -8.5446, 'Metal',    false, 'user',  '2026-09-26 13:00:00+02', 0),
(8,  'Parque Eugenio Granell',      'Fronte ao estanque, moi frecuentado por estudantes.',                  42.8740, -8.5560, 'Metal',    false, 'admin', '2026-09-27 18:00:00+02', 0),
(9,  'Porche da ETSE',              'Baixo o porche da Escola de Enxeñaría: non se mollas.',                42.8754, -8.5585, 'Concrete', true,  'brais', '2026-09-28 10:15:00+02', 0),
(10, 'Parque de Galeras',           'Xunto ao río Sarela, fresco no verán.',                                42.8822, -8.5520, 'Wooden',   false, 'ana',   '2026-09-29 19:00:00+02', 0),
(11, 'Soportais da Rúa do Vilar',   'Pequeno banco de pedra baixo os soportais.',                           42.8790, -8.5443, 'Stone',    true,  'user',  '2026-09-30 12:30:00+02', 0),
(12, 'Parque da Música',            'Amplo e con moito sol pola mañá.',                                     42.8915, -8.5310, 'Metal',    false, 'brais', '2026-10-01 11:45:00+02', 0),
(13, 'Praza Roxa',                  'Ruidoso pero ben situado para quedar.',                                42.8752, -8.5517, 'Concrete', false, 'admin', '2026-10-02 20:00:00+02', 0),
(14, 'Monte do Gozo',               'O primeiro banco no que ver as torres da Catedral.',                   42.8875, -8.4950, 'Wooden',   false, 'ana',   '2026-10-03 08:30:00+02', 0),
(15, 'Parque de Vista Alegre',      'Entre árbores e esculturas.',                                          42.8857, -8.5320, 'Wooden',   false, 'user',  '2026-10-04 15:00:00+02', 0),
(16, 'Marquesiña da Praza de Galicia', 'Banco da parada de autobús.',                                       42.8761, -8.5447, 'Metal',    true,  'brais', '2026-10-05 07:50:00+02', 0),
(17, 'Paseo dos Leóns',             'Banco clásico de pedra da Alameda.',                                   42.8774, -8.5490, 'Stone',    false, 'admin', '2026-10-06 18:20:00+02', 0),
(18, 'Parque de Carlomagno',        'Barrio tranquilo, con zona infantil ao lado.',                         42.8930, -8.5370, 'Concrete', false, 'ana',   '2026-10-07 17:10:00+02', 0),
(19, 'Carballeira de San Lourenzo', 'Bosque centenario: sombra e silencio.',                                42.8805, -8.5560, 'Wooden',   true,  'user',  '2026-10-08 16:40:00+02', 0),
(20, 'Mirador da Almáciga',         'Vistas ao val do Sar e a Conxo.',                                      42.8740, -8.5395, 'Wooden',   false, 'brais', '2026-10-09 19:30:00+02', 0);

-- Avanza a secuencia para que os POST non choquen cos IDs inseridos a man
SELECT setval(pg_get_serial_sequence('benches', 'bench_id'), (SELECT MAX(bench_id) FROM benches));
