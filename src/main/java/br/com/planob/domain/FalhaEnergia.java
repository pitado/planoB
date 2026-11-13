package br.com.planob.domain;

import java.time.Instant;

public final class FalhaEnergia extends Ocorrencia {
    FalhaEnergia(Long id, Localizacao local, String descricao, int severidade,
                StatusOcorrencia status, Instant criadaEm) {
        super(id, TipoOcorrencia.FALHA_ENERGIA, local, descricao, severidade, status, criadaEm);
    }

    @Override
    protected int pesoOperacional() { return 20; }
}
