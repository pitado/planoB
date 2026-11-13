package br.com.planob.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** A conexão é configurada exclusivamente por variáveis de ambiente. */
public final class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("DB_URL deve apontar para PostgreSQL via JDBC.");
        }
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database doAmbiente() {
        return new Database(
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/plano_b"),
                System.getenv().getOrDefault("DB_USER", "planob"),
                System.getenv().getOrDefault("DB_PASSWORD", "planob_dev"));
    }

    public Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public boolean disponivel() {
        try (Connection connection = abrir()) {
            return connection.isValid(2);
        } catch (SQLException exception) {
            return false;
        }
    }
}
