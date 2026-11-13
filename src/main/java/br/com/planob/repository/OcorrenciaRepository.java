package br.com.planob.repository;

import br.com.planob.domain.MudancaStatus;
import br.com.planob.domain.Ocorrencia;
import br.com.planob.domain.StatusOcorrencia;
import br.com.planob.domain.TipoOcorrencia;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OcorrenciaRepository {
    long criar(Ocorrencia ocorrencia);
    Optional<Ocorrencia> buscar(long id);
    List<Ocorrencia> listar(StatusOcorrencia status, TipoOcorrencia tipo);
    boolean atualizar(Ocorrencia ocorrencia);
    boolean excluir(long id);
    Map<StatusOcorrencia, Long> contarPorStatus();
    List<MudancaStatus> historico(long ocorrenciaId);
}
