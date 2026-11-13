package br.com.planob.web;

import br.com.planob.config.Database;
import br.com.planob.domain.AcaoRecuperacao;
import br.com.planob.domain.StatusOcorrencia;
import br.com.planob.domain.TipoOcorrencia;
import br.com.planob.repository.AcaoRepository;
import br.com.planob.repository.DataAccessException;
import br.com.planob.repository.OcorrenciaRepository;
import br.com.planob.service.OcorrenciaService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Interface demonstrativa local: sem autenticação e sem publicação pública. */
public final class WebApp {
    private final Database database;
    private final OcorrenciaRepository ocorrencias;
    private final AcaoRepository acoes;
    private final OcorrenciaService service;
    private final WebPages paginas;
    private HttpServer server;
    private ExecutorService executor;

    public WebApp(Database database, OcorrenciaRepository ocorrencias, AcaoRepository acoes) {
        this.database = Objects.requireNonNull(database);
        this.ocorrencias = Objects.requireNonNull(ocorrencias);
        this.acoes = Objects.requireNonNull(acoes);
        this.service = new OcorrenciaService(ocorrencias);
        this.paginas = new WebPages(ocorrencias, acoes, service);
    }

    public void iniciar(int porta) throws IOException {
        if (server != null) throw new IllegalStateException("Servidor já iniciado.");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        server.createContext("/", this::atender);
        executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.start();
    }

    public void parar() {
        if (server != null) {
            server.stop(0);
            executor.shutdownNow();
            executor = null;
            server = null;
        }
    }

    private void atender(HttpExchange troca) throws IOException {
        try {
            String caminho = troca.getRequestURI().getPath();
            String metodo = troca.getRequestMethod();
            if (metodo.equals("GET") && caminho.equals("/assets/site.css")) {
                css(troca); return;
            }
            if (metodo.equals("GET") && caminho.equals("/health")) {
                boolean saudavel = database.disponivel();
                responder(troca, saudavel ? 200 : 503,
                        saudavel ? "OK" : "Indisponível", "text/plain"); return;
            }
            if (metodo.equals("GET")) {
                if (caminho.equals("/")) { responderHtml(troca, paginas.painel()); return; }
                if (caminho.equals("/ocorrencias")) { responderHtml(troca, paginas.listagem(
                        enumOpcional(parametros(troca.getRequestURI().getRawQuery()).get("status"), StatusOcorrencia.class),
                        enumOpcional(parametros(troca.getRequestURI().getRawQuery()).get("tipo"), TipoOcorrencia.class))); return; }
                if (caminho.equals("/ocorrencias/nova")) { responderHtml(troca, paginas.formulario(null)); return; }
                if (caminho.equals("/sobre")) { responderHtml(troca, paginas.sobre()); return; }
            }
            if (metodo.equals("POST") && caminho.equals("/ocorrencias")) {
                Map<String, String> campos = formularioPost(troca);
                long id = service.cadastrar(tipo(campos), obrigatorio(campos,"bairro"), obrigatorio(campos,"logradouro"),
                        obrigatorio(campos,"descricao"), severidade(campos));
                redirecionar(troca, "/ocorrencias/" + id); return;
            }
            if (caminho.matches("/ocorrencias/[0-9]+(?:/.*)?")) {
                String[] partes = caminho.split("/");
                long id = Long.parseLong(partes[2]);
                if (metodo.equals("GET") && partes.length == 3) { responderHtml(troca, paginas.detalhe(id)); return; }
                if (metodo.equals("GET") && partes.length == 4 && partes[3].equals("editar")) {
                    responderHtml(troca, paginas.formulario(service.consultar(id))); return;
                }
                if (metodo.equals("GET") && partes.length == 4 && partes[3].equals("excluir")) {
                    responderHtml(troca, paginas.confirmarExclusao(service.consultar(id))); return;
                }
                if (metodo.equals("POST") && partes.length == 4) {
                    Map<String, String> campos = formularioPost(troca);
                    switch (partes[3]) {
                        case "atualizar" -> service.atualizar(id, tipo(campos), obrigatorio(campos,"bairro"),
                                obrigatorio(campos,"logradouro"), obrigatorio(campos,"descricao"),
                                severidade(campos), StatusOcorrencia.valueOf(obrigatorio(campos,"status")));
                        case "excluir" -> { service.excluir(id); redirecionar(troca, "/ocorrencias"); return; }
                        case "acoes" -> { service.consultar(id); acoes.criar(AcaoRecuperacao.nova(id,
                                obrigatorio(campos,"titulo"), obrigatorio(campos,"responsavel"))); }
                        default -> { erro(troca, 404, "Rota não encontrada."); return; }
                    }
                    redirecionar(troca, "/ocorrencias/" + id); return;
                }
                if (metodo.equals("POST") && partes.length == 6 && partes[3].equals("acoes")
                        && partes[5].equals("concluir")) {
                    formularioPost(troca); // valida o limite da requisição
                    service.consultar(id);
                    long acaoId = Long.parseLong(partes[4]);
                    if (!acoes.concluir(acaoId, id)) throw new NoSuchElementException("Ação não encontrada ou já concluída.");
                    redirecionar(troca, "/ocorrencias/" + id); return;
                }
            }
            erro(troca, 404, "Página não encontrada.");
        } catch (IllegalArgumentException error) {
            erro(troca, 400, error.getMessage());
        } catch (NoSuchElementException error) {
            erro(troca, 404, error.getMessage());
        } catch (DataAccessException error) {
            System.err.println("Falha de banco: " + error.getMessage());
            erro(troca, 503, "Não foi possível acessar os dados. Verifique o PostgreSQL.");
        } catch (Exception error) {
            System.err.println("Erro inesperado: " + error.getClass().getSimpleName());
            erro(troca, 500, "Não foi possível concluir a solicitação.");
        } finally {
            troca.close();
        }
    }

    private void css(HttpExchange troca) throws IOException {
        try (InputStream recurso = WebApp.class.getResourceAsStream("/static/site.css")) {
            if (recurso == null) { erro(troca, 404, "Estilo indisponível."); return; }
            byte[] bytes = recurso.readAllBytes();
            troca.getResponseHeaders().set("Content-Type", "text/css; charset=utf-8");
            troca.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            troca.sendResponseHeaders(200, bytes.length);
            troca.getResponseBody().write(bytes);
        }
    }

    private static Map<String, String> formularioPost(HttpExchange troca) throws IOException {
        String tipo = troca.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Formulário inválido.");
        }
        byte[] conteudo = troca.getRequestBody().readNBytes(8193);
        if (conteudo.length > 8192) throw new IllegalArgumentException("Formulário excede o limite permitido.");
        return parametros(new String(conteudo, StandardCharsets.UTF_8));
    }

    private static Map<String, String> parametros(String parametros) {
        java.util.HashMap<String, String> resultado = new java.util.HashMap<>();
        if (parametros == null || parametros.isBlank()) return resultado;
        for (String item : parametros.split("&")) {
            String[] par = item.split("=", 2);
            String chave = URLDecoder.decode(par[0], StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(par.length == 2 ? par[1] : "", StandardCharsets.UTF_8);
            resultado.put(chave, valor);
        }
        return resultado;
    }

    private static String obrigatorio(Map<String, String> campos, String chave) {
        String valor = campos.get(chave);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Campo obrigatório: " + chave);
        return valor;
    }

    private static TipoOcorrencia tipo(Map<String, String> campos) {
        return TipoOcorrencia.valueOf(obrigatorio(campos, "tipo"));
    }

    private static int severidade(Map<String, String> campos) {
        return Integer.parseInt(obrigatorio(campos, "severidade"));
    }

    private static <T extends Enum<T>> T enumOpcional(String valor, Class<T> classe) {
        return valor == null || valor.isBlank() ? null : Enum.valueOf(classe, valor);
    }

    private static void responderHtml(HttpExchange troca, String conteudo) throws IOException {
        responder(troca, 200, conteudo, "text/html");
    }

    private static void erro(HttpExchange troca, int codigo, String mensagem) throws IOException {
        responder(troca, codigo, Html.layout("Aviso", "<div class=\"page-head\"><div><h1>" + codigo
                + "</h1><p>" + Html.escape(mensagem) + "</p><a class=\"btn dark\" href=\"/\">Voltar ao início</a></div></div>", ""), "text/html");
    }

    private static void responder(HttpExchange troca, int codigo, String conteudo, String tipo) throws IOException {
        byte[] resposta = conteudo.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        troca.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; style-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
        troca.sendResponseHeaders(codigo, resposta.length);
        troca.getResponseBody().write(resposta);
    }

    private static void redirecionar(HttpExchange troca, String local) throws IOException {
        troca.getResponseHeaders().set("Location", local);
        troca.sendResponseHeaders(303, -1);
    }
}
