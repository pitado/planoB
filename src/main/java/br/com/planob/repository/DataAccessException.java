package br.com.planob.repository;

/** Falha de acesso ao banco; encapsula SQLException sem expor detalhes de JDBC às outras camadas. */
public final class DataAccessException extends RuntimeException {
    public DataAccessException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
