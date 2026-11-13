package br.com.planob.domain;

import java.time.Instant;

public final class DanoEstrutural extends Ocorrencia {
    DanoEstrutural(Long id, Localizacao local, String descricao, int severidade,
                  StatusOcorrencia status, Instant criadaEm) {
        super(id, TipoOcorrencia.DANO_ESTRUTURAL, local, descricao, severidade, status, criadaEm);
    }

    @Override
    protected int pesoOperacional() { return 25; }
}
