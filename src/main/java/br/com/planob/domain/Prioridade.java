package br.com.planob.domain;

public enum Prioridade {
    BAIXA("Baixa"), MEDIA("Média"), ALTA("Alta"), CRITICA("Crítica");

    private final String descricao;

    Prioridade(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public static Prioridade doIndice(int indice) {
        if (indice >= 85) return CRITICA;
        if (indice >= 65) return ALTA;
        if (indice >= 40) return MEDIA;
        return BAIXA;
    }
}
