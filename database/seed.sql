-- DADOS FICTICIOS para demonstracao; nao representam ocorrencias reais.
-- Aplicar somente uma vez em banco vazio (compose.yaml ja faz isso na primeira subida).
INSERT INTO ocorrencias (tipo, bairro, logradouro, descricao, severidade, status) VALUES
 ('VIA_OBSTRUIDA', 'Bairro Exemplo', 'Rua Didatica, quadra A', 'Exemplo simulado de via parcialmente bloqueada apos temporal.', 3, 'EM_ANALISE'),
 ('FALHA_ENERGIA', 'Vila Modelo',    'Avenida Ficticia, quadra B', 'Exemplo simulado de interrupcao no fornecimento eletrico.', 4, 'EM_RECUPERACAO'),
 ('ALAGAMENTO',    'Jardim Teste',   'Rua Ilustrativa, quadra C', 'Exemplo simulado de acumulo de agua em via publica.', 2, 'ABERTA'),
 ('DANO_ESTRUTURAL','Centro Ficticio','Travessa Simulada, 100', 'Exemplo simulado de rachadura em muro de contencao.', 5, 'RESOLVIDA');

INSERT INTO acoes_recuperacao (ocorrencia_id, titulo, responsavel, concluida)
SELECT id, 'Documentar ponto de bloqueio', 'Equipe de demonstracao', FALSE FROM ocorrencias WHERE logradouro = 'Rua Didatica, quadra A';
INSERT INTO acoes_recuperacao (ocorrencia_id, titulo, responsavel, concluida)
SELECT id, 'Registrar trecho sem energia', 'Equipe de demonstracao', TRUE FROM ocorrencias WHERE logradouro = 'Avenida Ficticia, quadra B';
INSERT INTO acoes_recuperacao (ocorrencia_id, titulo, responsavel, concluida)
SELECT id, 'Acompanhar restabelecimento', 'Equipe de demonstracao', FALSE FROM ocorrencias WHERE logradouro = 'Avenida Ficticia, quadra B';

-- Historico coerente com as transicoes permitidas pelo dominio.
INSERT INTO historico_status (ocorrencia_id, status_anterior, status_novo)
SELECT id, NULL, 'ABERTA' FROM ocorrencias;
INSERT INTO historico_status (ocorrencia_id, status_anterior, status_novo)
SELECT id, 'ABERTA', 'EM_ANALISE' FROM ocorrencias WHERE status IN ('EM_ANALISE','EM_RECUPERACAO','RESOLVIDA');
INSERT INTO historico_status (ocorrencia_id, status_anterior, status_novo)
SELECT id, 'EM_ANALISE', 'EM_RECUPERACAO' FROM ocorrencias WHERE status = 'EM_RECUPERACAO';
INSERT INTO historico_status (ocorrencia_id, status_anterior, status_novo)
SELECT id, 'EM_ANALISE', 'RESOLVIDA' FROM ocorrencias WHERE status = 'RESOLVIDA';
