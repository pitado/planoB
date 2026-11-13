package br.com.planob.domain;

public record Localizacao(String bairro, String logradouro) {
    public Localizacao {
        bairro = Validacao.texto(bairro, "Bairro", 120);
        logradouro = Validacao.texto(logradouro, "Logradouro", 180);
    }
}
