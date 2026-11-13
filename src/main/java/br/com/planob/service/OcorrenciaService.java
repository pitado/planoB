package br.com.planob.service;

import br.com.planob.domain.Localizacao;
import br.com.planob.domain.Ocorrencia;
import br.com.planob.domain.OcorrenciaFactory;
import br.com.planob.domain.StatusOcorrencia;
import br.com.planob.domain.TipoOcorrencia;
import br.com.planob.domain.Validacao;
import br.com.planob.repository.OcorrenciaRepository;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Casos de uso independentes da camada HTTP e JDBC. */
public final class OcorrenciaService {
    private final OcorrenciaRepository repository;

    public OcorrenciaService(OcorrenciaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public long cadastrar(TipoOcorrencia tipo, String bairro, String logradouro, String descricao, int severidade) {
        return repository.criar(OcorrenciaFactory.nova(tipo, new Localizacao(bairro, logradouro), descricao, severidade));
    }

    public Ocorrencia consultar(long id) {
        return repository.buscar(Validacao.idPositivo(id))
                .orElseThrow(() -> new NoSuchElementException("Ocorrência não encontrada."));
    }

    public void atualizar(long id, TipoOcorrencia tipo, String bairro, String logradouro, String descricao,
                          int severidade, StatusOcorrencia status) {
        Ocorrencia anterior = consultar(id);
        Ocorrencia atualizada = OcorrenciaFactory.reconstituir(anterior.id(), tipo,
                new Localizacao(bairro, logradouro), descricao, severidade, status, anterior.criadaEm());
        if (!repository.atualizar(atualizada)) {
            throw new NoSuchElementException("Ocorrência não encontrada.");
        }
    }

    public void excluir(long id) {
        if (!repository.excluir(Validacao.idPositivo(id))) {
            throw new NoSuchElementException("Ocorrência não encontrada.");
        }
    }
}
