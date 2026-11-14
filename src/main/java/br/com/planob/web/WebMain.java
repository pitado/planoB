package br.com.planob.web;

import java.util.concurrent.CountDownLatch;

/**
 * Inicialização isolada da interface da Sprint Lucas.
 * Memória volátil (sem PostgreSQL): somente demonstração local.
 */
public final class WebMain {
    private WebMain(){}

    public static void main(String[] args) throws Exception {
        int port=Integer.parseInt(System.getenv().getOrDefault("PORT","8080"));
        var registros=new DemoRepositories.Ocorrencias();
        var acoes=new DemoRepositories.Acoes();
        var web=new WebApp(registros,acoes);
        int actual=web.iniciar(port);
        Runtime.getRuntime().addShutdownHook(new Thread(web::parar));
        System.out.println("Plano B — interface de teste: http://127.0.0.1:"+actual);
        System.out.println("Dados voláteis; reiniciar apaga as ocorrências. Não usar em produção.");
        new CountDownLatch(1).await();
    }
}
