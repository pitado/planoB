package br.com.planob.repository;

import br.com.planob.domain.AcaoRecuperacao;
import java.util.List;

public interface AcaoRepository {
    long criar(AcaoRecuperacao acao);
    List<AcaoRecuperacao> listarPorOcorrencia(long ocorrenciaId);
    boolean concluir(long acaoId, long ocorrenciaId);
}
