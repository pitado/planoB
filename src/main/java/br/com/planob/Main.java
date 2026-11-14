package br.com.planob;

import br.com.planob.config.Database;
import br.com.planob.repository.JdbcAcaoRepository;
import br.com.planob.repository.JdbcOcorrenciaRepository;
import br.com.planob.web.WebApp;
import java.util.concurrent.CountDownLatch;

/**
 * Entrada da aplicação INTEGRADA: HTTP -> serviços -> JDBC -> PostgreSQL.
 * O modo volátil isolado da Sprint Lucas permanece em web.WebMain para testes.
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        Database database = Database.doAmbiente();
        if (!database.disponivel()) {
            throw new IllegalStateException(
                    "PostgreSQL indisponível. Inicie 'docker compose up -d postgres' e confira DB_URL, DB_USER, DB_PASSWORD.");
        }
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        WebApp web = new WebApp(new JdbcOcorrenciaRepository(database), new JdbcAcaoRepository(database));
        int portaAtiva = web.iniciar(porta);
        Runtime.getRuntime().addShutdownHook(new Thread(web::parar));
        System.out.println("Plano B — HTTP + PostgreSQL: http://127.0.0.1:" + portaAtiva);
        System.out.println("Protótipo acadêmico local: use dados fictícios. Não publicar sem autenticação.");
        new CountDownLatch(1).await();
    }
}
