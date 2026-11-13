CREATE TABLE IF NOT EXISTS ocorrencias (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('FALHA_ENERGIA','VIA_OBSTRUIDA','ALAGAMENTO','DANO_ESTRUTURAL')),
    bairro VARCHAR(120) NOT NULL,
    logradouro VARCHAR(180) NOT NULL,
    descricao VARCHAR(1200) NOT NULL,
    severidade INTEGER NOT NULL CHECK (severidade BETWEEN 1 AND 5),
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTA' CHECK (status IN ('ABERTA','EM_ANALISE','EM_RECUPERACAO','RESOLVIDA')),
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_status ON ocorrencias(status);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_tipo ON ocorrencias(tipo);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_bairro ON ocorrencias(bairro);

CREATE TABLE IF NOT EXISTS acoes_recuperacao (
    id BIGSERIAL PRIMARY KEY,
    ocorrencia_id BIGINT NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    titulo VARCHAR(160) NOT NULL,
    responsavel VARCHAR(120) NOT NULL,
    concluida BOOLEAN NOT NULL DEFAULT FALSE,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_acoes_ocorrencia ON acoes_recuperacao(ocorrencia_id);

CREATE TABLE IF NOT EXISTS historico_status (
    id BIGSERIAL PRIMARY KEY,
    ocorrencia_id BIGINT NOT NULL REFERENCES ocorrencias(id) ON DELETE CASCADE,
    status_anterior VARCHAR(30),
    status_novo VARCHAR(30) NOT NULL,
    alterada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_historico_ocorrencia ON historico_status(ocorrencia_id);
