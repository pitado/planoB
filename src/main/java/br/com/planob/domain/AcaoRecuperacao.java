package br.com.planob.domain;

import java.time.Instant;
import java.util.Objects;

/** Composição: uma ocorrência possui muitas ações de recuperação. */
public record AcaoRecuperacao(Long id, long ocorrenciaId, String titulo, String responsavel,
                             boolean concluida, Instant criadaEm) {
    public AcaoRecuperacao {
        if (id != null) Validacao.idPositivo(id);
        Validacao.idPositivo(ocorrenciaId);
        titulo = Validacao.texto(titulo, "Título", 160);
        responsavel = Validacao.texto(responsavel, "Responsável", 120);
        Objects.requireNonNull(criadaEm, "Data obrigatória.");
    }

    public static AcaoRecuperacao nova(long ocorrenciaId, String titulo, String responsavel) {
        return new AcaoRecuperacao(null, ocorrenciaId, titulo, responsavel, false, Instant.now());
    }
}
