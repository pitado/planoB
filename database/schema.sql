-- Plano B - esquema PostgreSQL 16 (Sprint 3).
-- Idempotente: pode ser reaplicado sem apagar dados.

CREATE TABLE IF NOT EXISTS ocorrencias (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo        VARCHAR(30)   NOT NULL,
    bairro      VARCHAR(120)  NOT NULL,
    logradouro  VARCHAR(180)  NOT NULL,
    descricao   VARCHAR(1200) NOT NULL,
    severidade  SMALLINT      NOT NULL,
    status      VARCHAR(30)   NOT NULL DEFAULT 'ABERTA',
    criada_em   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ocorrencias_tipo CHECK (tipo IN ('FALHA_ENERGIA','VIA_OBSTRUIDA','ALAGAMENTO','DANO_ESTRUTURAL')),
    CONSTRAINT ck_ocorrencias_status CHECK (status IN ('ABERTA','EM_ANALISE','EM_RECUPERACAO','RESOLVIDA')),
    CONSTRAINT ck_ocorrencias_severidade CHECK (severidade BETWEEN 1 AND 5),
    CONSTRAINT ck_ocorrencias_textos CHECK (length(btrim(bairro)) > 0 AND length(btrim(logradouro)) > 0
                                            AND length(btrim(descricao)) > 0)
);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_status_tipo ON ocorrencias (status, tipo);
CREATE INDEX IF NOT EXISTS idx_ocorrencias_criada_em ON ocorrencias (criada_em DESC, id DESC);

CREATE TABLE IF NOT EXISTS acoes_recuperacao (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocorrencia_id BIGINT       NOT NULL,
    titulo        VARCHAR(160) NOT NULL,
    responsavel   VARCHAR(120) NOT NULL,
    concluida     BOOLEAN      NOT NULL DEFAULT FALSE,
    criada_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_acoes_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES ocorrencias (id) ON DELETE CASCADE,
    CONSTRAINT ck_acoes_textos CHECK (length(btrim(titulo)) > 0 AND length(btrim(responsavel)) > 0)
);
CREATE INDEX IF NOT EXISTS idx_acoes_ocorrencia ON acoes_recuperacao (ocorrencia_id, concluida, criada_em);

CREATE TABLE IF NOT EXISTS historico_status (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocorrencia_id   BIGINT      NOT NULL,
    status_anterior VARCHAR(30),
    status_novo     VARCHAR(30) NOT NULL,
    alterada_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES ocorrencias (id) ON DELETE CASCADE,
    CONSTRAINT ck_historico_novo CHECK (status_novo IN ('ABERTA','EM_ANALISE','EM_RECUPERACAO','RESOLVIDA')),
    CONSTRAINT ck_historico_anterior CHECK (status_anterior IS NULL
        OR status_anterior IN ('ABERTA','EM_ANALISE','EM_RECUPERACAO','RESOLVIDA')),
    CONSTRAINT ck_historico_mudanca CHECK (status_anterior IS DISTINCT FROM status_novo)
);
CREATE INDEX IF NOT EXISTS idx_historico_ocorrencia ON historico_status (ocorrencia_id, alterada_em DESC, id DESC);
