package br.com.planob.domain;

import java.util.Objects;

/**
 * Regras de fluxo das ocorrências. Não representam protocolos de emergência.
 *
 * Uma ocorrência inicia ABERTA. Depois da análise, pode seguir para recuperação
 * ou ser resolvida diretamente quando não existe ação pendente.
 * Uma resolução pode ser reaberta para nova análise.
 */
public final class PoliticaTransicaoStatus {
    private PoliticaTransicaoStatus() {}

    public static boolean permite(StatusOcorrencia atual, StatusOcorrencia destino) {
        Objects.requireNonNull(atual, "Status atual obrigatório.");
        Objects.requireNonNull(destino, "Novo status obrigatório.");

        if (atual == destino) return true;

        return switch (atual) {
            case ABERTA -> destino == StatusOcorrencia.EM_ANALISE;
            case EM_ANALISE -> destino == StatusOcorrencia.EM_RECUPERACAO
                    || destino == StatusOcorrencia.RESOLVIDA;
            case EM_RECUPERACAO -> destino == StatusOcorrencia.EM_ANALISE
                    || destino == StatusOcorrencia.RESOLVIDA;
            case RESOLVIDA -> destino == StatusOcorrencia.EM_ANALISE;
        };
    }

    public static void exigirTransicaoValida(StatusOcorrencia atual, StatusOcorrencia destino) {
        if (!permite(atual, destino)) {
            throw new IllegalArgumentException(
                    "Transição de situação não permitida: " + atual.descricao()
                            + " → " + destino.descricao() + ".");
        }
    }
}
