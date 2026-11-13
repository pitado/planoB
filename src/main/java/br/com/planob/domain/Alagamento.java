package br.com.planob.domain;

import java.time.Instant;

public final class Alagamento extends Ocorrencia {
    Alagamento(Long id, Localizacao local, String descricao, int severidade,
              StatusOcorrencia status, Instant criadaEm) {
        super(id, TipoOcorrencia.ALAGAMENTO, local, descricao, severidade, status, criadaEm);
    }

    @Override
    protected int pesoOperacional() { return 10; }
}
