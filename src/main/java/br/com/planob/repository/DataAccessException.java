package br.com.planob.repository;

public final class DataAccessException extends RuntimeException {
    public DataAccessException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
