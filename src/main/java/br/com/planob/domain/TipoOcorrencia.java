package br.com.planob.domain;

public enum TipoOcorrencia {
    FALHA_ENERGIA("Falha de energia"),
    VIA_OBSTRUIDA("Via obstruída"),
    ALAGAMENTO("Alagamento"),
    DANO_ESTRUTURAL("Dano estrutural");

    private final String descricao;

    TipoOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
