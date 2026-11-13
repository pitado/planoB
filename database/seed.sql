-- CENÁRIOS FICTÍCIOS para demonstração; não representam ocorrências confirmadas.
INSERT INTO ocorrencias (tipo, bairro, logradouro, descricao, severidade, status) VALUES
('VIA_OBSTRUIDA', 'Zona 03', 'Rua Exemplo, quadra A', 'Exemplo simulado de via parcialmente bloqueada após um temporal.', 3, 'EM_ANALISE'),
('FALHA_ENERGIA', 'Vila Operária', 'Avenida Modelo, quadra B', 'Exemplo simulado de interrupção no fornecimento elétrico.', 4, 'EM_RECUPERACAO'),
('ALAGAMENTO', 'Jardim Fictício', 'Rua Didática, quadra C', 'Exemplo simulado de acúmulo de água em via pública.', 2, 'ABERTA');
INSERT INTO acoes_recuperacao (ocorrencia_id, titulo, responsavel)
SELECT id, 'Documentar ponto de bloqueio e comunicar o canal oficial', 'Equipe de demonstração'
FROM ocorrencias WHERE logradouro = 'Rua Exemplo, quadra A';
INSERT INTO historico_status (ocorrencia_id, status_anterior, status_novo)
SELECT id, NULL, status FROM ocorrencias;
