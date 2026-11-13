package br.com.planob.domain;

public final class Validacao {
    private Validacao() {}

    public static String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        String limpo = valor.strip();
        if (limpo.length() > maximo) {
            throw new IllegalArgumentException(campo + " excede " + maximo + " caracteres.");
        }
        return limpo;
    }

    public static long idPositivo(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identificador inválido.");
        }
        return id;
    }
}
