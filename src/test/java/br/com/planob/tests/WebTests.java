package br.com.planob.tests;

import br.com.planob.config.Database;
import br.com.planob.web.WebApp;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** Testa a interface HTTP inteira contra dublês do repositório. */
public final class WebTests {
    private static int verificacoes;

    public static void main(String[] args) throws Exception {
        int porta;
        try (ServerSocket portaLivre = new ServerSocket(0)) { porta = portaLivre.getLocalPort(); }
        InMemoryFixtures.Ocorrencias ocorrencias = new InMemoryFixtures.Ocorrencias();
        InMemoryFixtures.Acoes acoes = new InMemoryFixtures.Acoes();
        WebApp web = new WebApp(new Database("jdbc:postgresql://localhost:5432/plano_b", "x", "x"), ocorrencias, acoes);
        try {
            web.iniciar(porta);
            String base = "http://127.0.0.1:" + porta;
            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            resposta(http, base, "GET", "/", null, 200, "Panorama das ocorrências");
            resposta(http, base, "GET", "/ocorrencias/nova", null, 200, "Registrar ocorrência");
            resposta(http, base, "GET", "/assets/site.css", null, 200, "--olive");
            resposta(http, base, "POST", "/ocorrencias",
                    "tipo=VIA_OBSTRUIDA&bairro=Centro&logradouro=Rua+Modelo&descricao=Caso+simulado&severidade=3", 303, null);
            verificar(ocorrencias.listar(null, null).size() == 1, "POST cria no repositório");
            resposta(http, base, "GET", "/ocorrencias/1", null, 200, "Caso simulado");
            resposta(http, base, "POST", "/ocorrencias/1/atualizar",
                    "tipo=ALAGAMENTO&bairro=Centro&logradouro=Rua+Modelo&descricao=Atualizado&severidade=4&status=EM_ANALISE", 303, null);
            resposta(http, base, "GET", "/ocorrencias/1", null, 200, "Atualizado");
            resposta(http, base, "POST", "/ocorrencias/1/acoes", "titulo=Contatar+equipe&responsavel=Voluntario", 303, null);
            resposta(http, base, "POST", "/ocorrencias/1/acoes/1/concluir", "", 303, null);
            resposta(http, base, "GET", "/ocorrencias/1", null, 200, "Concluída");
            resposta(http, base, "POST", "/ocorrencias",
                    "tipo=ALAGAMENTO&bairro=Centro&logradouro=Rua&descricao=Erro&severidade=9", 400, null);
            resposta(http, base, "POST", "/ocorrencias/1/excluir", "", 303, null);
            resposta(http, base, "GET", "/ocorrencias/1", null, 404, "não encontrada");
            resposta(http, base, "GET", "/caminho-invalido", null, 404, null);
            System.out.println("APROVADO: " + verificacoes + " verificações HTTP e fluxo CRUD.");
        } finally {
            web.parar();
        }
    }

    private static void resposta(HttpClient http, String base, String metodo, String rota,
                                String formulario, int esperado, String contem) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + rota));
        if (metodo.equals("POST")) {
            request.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(formulario));
        } else request.GET();
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        verificar(response.statusCode() == esperado,
                metodo + " " + rota + " deveria retornar " + esperado + " e retornou " + response.statusCode());
        if (contem != null) verificar(response.body().contains(contem), "Corpo de " + rota + " deve conter " + contem);
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
        verificacoes++;
    }
}
