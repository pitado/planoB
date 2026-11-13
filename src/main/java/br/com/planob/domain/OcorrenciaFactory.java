package br.com.planob.domain;

import java.time.Instant;
import java.util.Objects;

public final class OcorrenciaFactory {
    private OcorrenciaFactory() {}

    public static Ocorrencia nova(TipoOcorrencia tipo, Localizacao local, String descricao, int severidade) {
        return reconstituir(null, tipo, local, descricao, severidade, StatusOcorrencia.ABERTA, Instant.now());
    }

    public static Ocorrencia reconstituir(Long id, TipoOcorrencia tipo, Localizacao local,
                                         String descricao, int severidade, StatusOcorrencia status, Instant criadaEm) {
        return switch (Objects.requireNonNull(tipo, "Tipo obrigatório.")) {
            case FALHA_ENERGIA -> new FalhaEnergia(id, local, descricao, severidade, status, criadaEm);
            case VIA_OBSTRUIDA -> new ViaObstruida(id, local, descricao, severidade, status, criadaEm);
            case ALAGAMENTO -> new Alagamento(id, local, descricao, severidade, status, criadaEm);
            case DANO_ESTRUTURAL -> new DanoEstrutural(id, local, descricao, severidade, status, criadaEm);
        };
    }
}
