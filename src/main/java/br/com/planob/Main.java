package br.com.planob;

import br.com.planob.config.Database;
import br.com.planob.repository.JdbcAcaoRepository;
import br.com.planob.repository.JdbcOcorrenciaRepository;
import br.com.planob.web.WebApp;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        Database database = Database.doAmbiente();
        // Falha cedo se o banco estiver indisponível ou o schema não tiver sido aplicado.
        try (Connection conexao = database.abrir(); Statement sql = conexao.createStatement();
             ResultSet ignorado = sql.executeQuery("SELECT COUNT(*) FROM ocorrencias")) {
            System.out.println("Conexão PostgreSQL validada.");
        }

        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        WebApp web = new WebApp(database, new JdbcOcorrenciaRepository(database), new JdbcAcaoRepository(database));
        web.iniciar(porta);
        System.out.println("Plano B em http://127.0.0.1:" + porta);
        Runtime.getRuntime().addShutdownHook(new Thread(web::parar));
    }
}
