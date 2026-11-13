package br.com.planob.domain;

import java.time.Instant;

public final class ViaObstruida extends Ocorrencia {
    ViaObstruida(Long id, Localizacao local, String descricao, int severidade,
                StatusOcorrencia status, Instant criadaEm) {
        super(id, TipoOcorrencia.VIA_OBSTRUIDA, local, descricao, severidade, status, criadaEm);
    }

    @Override
    protected int pesoOperacional() { return 15; }
}
