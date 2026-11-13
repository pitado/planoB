package br.com.planob.domain;

public enum StatusOcorrencia {
    ABERTA("Aberta"),
    EM_ANALISE("Em análise"),
    EM_RECUPERACAO("Em recuperação"),
    RESOLVIDA("Resolvida");

    private final String descricao;

    StatusOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
