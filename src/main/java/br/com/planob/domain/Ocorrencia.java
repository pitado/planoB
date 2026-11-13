package br.com.planob.domain;

import java.time.Instant;
import java.util.Objects;

/** Registro acadêmico: índice orientativo, não avaliação oficial de risco. */
public abstract class Ocorrencia {
    private final Long id;
    private final TipoOcorrencia tipo;
    private final Localizacao localizacao;
    private final String descricao;
    private final int severidade;
    private final StatusOcorrencia status;
    private final Instant criadaEm;

    protected Ocorrencia(Long id, TipoOcorrencia tipo, Localizacao localizacao, String descricao,
                         int severidade, StatusOcorrencia status, Instant criadaEm) {
        if (id != null) Validacao.idPositivo(id);
        this.id = id;
        this.tipo = Objects.requireNonNull(tipo, "Tipo obrigatório.");
        this.localizacao = Objects.requireNonNull(localizacao, "Localização obrigatória.");
        this.descricao = Validacao.texto(descricao, "Descrição", 1200);
        if (severidade < 1 || severidade > 5) {
            throw new IllegalArgumentException("Severidade deve ser de 1 a 5.");
        }
        this.severidade = severidade;
        this.status = Objects.requireNonNull(status, "Status obrigatório.");
        this.criadaEm = Objects.requireNonNull(criadaEm, "Data de criação obrigatória.");
    }

    /** Cada subtipo especializa o peso: demonstra herança e @Override. */
    protected abstract int pesoOperacional();

    public final int calcularIndiceImpacto() {
        return Math.min(100, severidade * 15 + pesoOperacional());
    }

    public final Prioridade prioridade() {
        return Prioridade.doIndice(calcularIndiceImpacto());
    }

    public Long id() { return id; }
    public TipoOcorrencia tipo() { return tipo; }
    public Localizacao localizacao() { return localizacao; }
    public String descricao() { return descricao; }
    public int severidade() { return severidade; }
    public StatusOcorrencia status() { return status; }
    public Instant criadaEm() { return criadaEm; }
}
