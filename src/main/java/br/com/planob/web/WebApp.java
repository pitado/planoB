package br.com.planob.web;

import br.com.planob.config.Database;
import br.com.planob.domain.AcaoRecuperacao;
import br.com.planob.domain.Ocorrencia;
import br.com.planob.domain.Prioridade;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Executors;

/** Interface demonstrativa local: sem autenticação e sem publicação pública. */
public final class WebApp {
    private final Database database;
    private final OcorrenciaRepository ocorrencias;
    private final AcaoRepository acoes;
    private final OcorrenciaService service;
    private HttpServer server;

    public WebApp(Database database, OcorrenciaRepository ocorrencias, AcaoRepository acoes) {
        this.database = Objects.requireNonNull(database);
        this.ocorrencias = Objects.requireNonNull(ocorrencias);
        this.acoes = Objects.requireNonNull(acoes);
        this.service = new OcorrenciaService(ocorrencias);
    }

    public void iniciar(int porta) throws IOException {
        if (server != null) throw new IllegalStateException("Servidor já iniciado.");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        server.createContext("/", this::atender);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    public void parar() {
        if (server != null) {
            server.stop(0);
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
                responder(troca, database.disponivel() ? 200 : 503,
                        database.disponivel() ? "OK" : "Indisponível", "text/plain"); return;
            }
            if (metodo.equals("GET")) {
                if (caminho.equals("/")) { responderHtml(troca, painel()); return; }
                if (caminho.equals("/ocorrencias")) { responderHtml(troca, listagem(troca)); return; }
                if (caminho.equals("/ocorrencias/nova")) { responderHtml(troca, formulario(null)); return; }
                if (caminho.equals("/sobre")) { responderHtml(troca, sobre()); return; }
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
                if (metodo.equals("GET") && partes.length == 3) { responderHtml(troca, detalhe(id)); return; }
                if (metodo.equals("GET") && partes.length == 4 && partes[3].equals("editar")) {
                    responderHtml(troca, formulario(service.consultar(id))); return;
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

    private String painel() {
        Map<StatusOcorrencia, Long> contagens = ocorrencias.contarPorStatus();
        long total = contagens.values().stream().mapToLong(Long::longValue).sum();
        List<Ocorrencia> ultimas = ocorrencias.listar(null, null);
        StringBuilder cards = new StringBuilder();
        cards.append(cartao("Registros", total, "Ocorrências cadastradas"));
        cards.append(cartao("Em aberto", contagens.get(StatusOcorrencia.ABERTA), "Aguardando análise"));
        cards.append(cartao("Em recuperação", contagens.get(StatusOcorrencia.EM_RECUPERACAO), "Ações em andamento"));
        cards.append(cartao("Resolvidas", contagens.get(StatusOcorrencia.RESOLVIDA), "Histórico registrado"));
        String body = """
                <section class="hero"><div><div class="eyebrow">CENTRAL DE CONTINUIDADE URBANA</div><h1>Quando o inesperado acontece,<br><em>existe um próximo passo.</em></h1>
                <p>Organize impactos, prioridades orientativas e ações de recuperação em um só lugar.</p>
                <a class="btn primary" href="/ocorrencias/nova">+ Registrar ocorrência</a><a class="btn ghost" href="/sobre">Conheça o projeto</a></div>
                <div class="hero-symbol">B<span>.</span></div></section>
                <div class="section-head"><div><div class="eyebrow">VISÃO OPERACIONAL</div><h2>Panorama das ocorrências</h2></div></div>
                <div class="metrics">%s</div><section class="surface"><div class="section-head"><h2>Registros recentes</h2><a href="/ocorrencias">Ver todas →</a></div>
                <div class="table-wrap"><table><thead><tr><th>Ocorrência</th><th>Bairro</th><th>Situação</th><th>Prioridade</th></tr></thead>
                <tbody>%s</tbody></table></div></section>
                <p class="disclaimer">Dados de demonstração. O índice é orientativo e não substitui análises técnicas, alertas ou atendimentos oficiais.</p>
                """.formatted(cards, linhas(ultimas.subList(0, Math.min(6, ultimas.size()))));
        return Html.layout("Visão geral", body, "inicio");
    }

    private static String cartao(String titulo, long numero, String legenda) {
        return "<div class=\"metric\"><span>" + Html.escape(titulo) + "</span><strong>" + numero + "</strong><small>"
                + Html.escape(legenda) + "</small></div>";
    }

    private String listagem(HttpExchange troca) {
        Map<String, String> filtros = parametros(troca.getRequestURI().getRawQuery());
        StatusOcorrencia status = enumOpcional(filtros.get("status"), StatusOcorrencia.class);
        TipoOcorrencia tipo = enumOpcional(filtros.get("tipo"), TipoOcorrencia.class);
        List<Ocorrencia> lista = ocorrencias.listar(status, tipo);
        String corpo = """
                <div class="page-head"><div><span class="eyebrow">ACOMPANHAMENTO</span><h1>Ocorrências</h1>
                <p>Registre e acompanhe impactos por bairro, tipo e situação.</p></div>
                <a class="btn primary" href="/ocorrencias/nova">+ Nova ocorrência</a></div>
                <section class="surface"><form method="get" action="/ocorrencias" class="filters"><label>Situação<select name="status">%s</select></label>
                <label>Tipo<select name="tipo">%s</select></label><button class="btn dark" type="submit">Filtrar</button>
                <a class="btn ghost" href="/ocorrencias">Limpar</a></form>
                <div class="table-wrap"><table><thead><tr><th>Ocorrência</th><th>Bairro</th><th>Situação</th><th>Prioridade</th></tr></thead>
                <tbody>%s</tbody></table></div></section><p class="disclaimer">Exibindo até 200 registros mais recentes.</p>
                """.formatted(Html.status(status, true), Html.tipos(tipo, true), linhas(lista));
        return Html.layout("Ocorrências", corpo, "ocorrencias");
    }

    private String detalhe(long id) {
        Ocorrencia o = service.consultar(id);
        List<AcaoRecuperacao> lista = acoes.listarPorOcorrencia(id);
        StringBuilder itens = new StringBuilder();
        for (AcaoRecuperacao acao : lista) {
            itens.append("<div class=\"action-item\"><div><strong>").append(Html.escape(acao.titulo()))
                    .append("</strong><small>Responsável: ").append(Html.escape(acao.responsavel())).append("</small></div>");
            if (acao.concluida()) {
                itens.append("<span class=\"completed\">Concluída</span>");
            } else {
                itens.append("<form method=\"post\" action=\"/ocorrencias/").append(id).append("/acoes/")
                        .append(acao.id()).append("/concluir\"><button type=\"submit\" class=\"btn small\">Concluir</button></form>");
            }
            itens.append("</div>");
        }
        if (lista.isEmpty()) itens.append("<p class=\"empty\">Nenhuma ação registrada. Adicione um próximo passo abaixo.</p>");
        StringBuilder historico = new StringBuilder();
        ocorrencias.historico(id).forEach(h -> historico.append("<li><strong>")
                .append(Html.escape(h.atual().descricao())).append("</strong><small>")
                .append(Html.data(h.alteradaEm())).append("</small></li>"));
        String corpo = """
                <div class="back"><a href="/ocorrencias">← Voltar às ocorrências</a></div>
                <div class="page-head"><div><span class="eyebrow">REGISTRO #%d</span><h1>%s</h1>
                <p>%s · %s</p></div><div class="page-actions"><a class="btn dark" href="/ocorrencias/%d/editar">Editar registro</a>
                <form method="post" action="/ocorrencias/%d/excluir"><button class="btn danger" type="submit">Excluir</button></form></div></div>
                <div class="columns"><section class="surface"><div class="section-head"><h2>Informações</h2>%s</div>
                <div class="details"><div><small>Severidade</small><strong>%d / 5</strong></div>
                <div><small>Prioridade orientativa</small><strong>%s · %d/100</strong></div>
                <div><small>Cadastrada em</small><strong>%s</strong></div></div><h3>Descrição</h3><p class="description">%s</p></section>
                <section class="surface"><div class="section-head"><h2>Histórico de situação</h2></div><ul class="history">%s</ul></section></div>
                <section class="surface"><div class="section-head"><h2>Ações de recuperação</h2><span class="tag">%d ação(ões)</span></div>%s
                <form method="post" action="/ocorrencias/%d/acoes" class="form-inline"><label>Tarefa<input name="titulo" maxlength="160" required placeholder="Próximo passo da recuperação"></label>
                <label>Responsável<input name="responsavel" maxlength="120" required placeholder="Equipe, pessoa ou estabelecimento"></label>
                <button class="btn primary" type="submit">Adicionar ação</button></form></section>
                <p class="disclaimer">Todas as informações exibidas pertencem ao protótipo acadêmico; não são alertas públicos verificados.</p>
                """.formatted(id, Html.escape(o.tipo().descricao()), Html.escape(o.localizacao().bairro()),
                Html.escape(o.localizacao().logradouro()), id, id, Html.badge(o.status()),
                o.severidade(), Html.escape(o.prioridade().descricao()), o.calcularIndiceImpacto(), Html.data(o.criadaEm()),
                Html.escape(o.descricao()), historico, lista.size(), itens, id);
        return Html.layout("Ocorrência #" + id, corpo, "ocorrencias");
    }

    private String formulario(Ocorrencia o) {
        boolean editar = o != null;
        String titulo = editar ? "Editar ocorrência" : "Registrar ocorrência";
        String action = editar ? "/ocorrencias/" + o.id() + "/atualizar" : "/ocorrencias";
        String status = editar ? "<label>Situação<select name=\"status\">" + Html.status(o.status(), false)
                + "</select></label>" : "<div class=\"hint\">Novas ocorrências começam com a situação Aberta.</div>";
        String corpo = """
                <div class="back"><a href="/ocorrencias">← Voltar às ocorrências</a></div>
                <div class="page-head"><div><span class="eyebrow">GESTÃO DE IMPACTOS</span><h1>%s</h1><p>Campos obrigatórios; use informações fictícias para demonstração.</p></div></div>
                <section class="surface form-surface"><form method="post" action="%s" class="form-grid">
                <label>Tipo de ocorrência<select name="tipo" required>%s</select></label>
                <label>Severidade<select name="severidade" required>%s</select></label>
                <label>Bairro<input name="bairro" maxlength="120" required value="%s" placeholder="Ex.: Zona 03"></label>
                <label>Logradouro<input name="logradouro" maxlength="180" required value="%s" placeholder="Rua e referência"></label>
                <label class="wide">Descrição<textarea name="descricao" maxlength="1200" required rows="5" placeholder="Descreva o impacto observado">%s</textarea></label>
                %s<div class="wide actions"><button class="btn primary" type="submit">%s</button><a class="btn ghost" href="/ocorrencias">Cancelar</a></div>
                </form></section><p class="disclaimer">Não compartilhe dados pessoais sensíveis. Este MVP não verifica relatos reais.</p>
                """.formatted(titulo, action, Html.tipos(editar ? o.tipo() : null, false),
                opcoesSeveridade(editar ? o.severidade() : 3), Html.escape(editar ? o.localizacao().bairro() : ""),
                Html.escape(editar ? o.localizacao().logradouro() : ""), Html.escape(editar ? o.descricao() : ""),
                status, editar ? "Salvar alterações" : "Registrar ocorrência");
        return Html.layout(titulo, corpo, "ocorrencias");
    }

    private static String opcoesSeveridade(int atual) {
        StringBuilder b = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            b.append("<option value=\"").append(i).append("\"").append(i == atual ? " selected" : "")
                    .append(">").append(i).append(" — ").append(switch (i) {
                        case 1 -> "Baixa"; case 2 -> "Moderada"; case 3 -> "Relevante";
                        case 4 -> "Elevada"; default -> "Muito elevada";
                    }).append("</option>");
        }
        return b.toString();
    }

    private static String linhas(List<Ocorrencia> lista) {
        if (lista.isEmpty()) return "<tr><td colspan=\"4\" class=\"empty\">Nenhuma ocorrência encontrada.</td></tr>";
        StringBuilder tabela = new StringBuilder();
        for (Ocorrencia item : lista) tabela.append(Html.resumo(item));
        return tabela.toString();
    }

    private String sobre() {
        String corpo = """
                <div class="page-head"><div><span class="eyebrow">UM PROJETO PARA O DEPOIS</span><h1>Sobre o Plano B</h1>
                <p>Preparação e recuperação urbana começam com informação organizada.</p></div></div>
                <section class="surface prose"><h2>Por que existe?</h2>
                <p>A proposta foi motivada pela experiência de eventos climáticos severos em Maringá, Paraná. O Plano B demonstra como registros de interrupções, histórico de situação e ações de recuperação podem apoiar a organização de um bairro ou estabelecimento.</p>
                <h2>Como funciona?</h2><p>Quatro categorias de ocorrências, severidade de 1 a 5, prioridade calculada por regras de domínio e acompanhamento das etapas de resolução. Cada ocorrência pode gerar diversas ações e registrar alterações de status.</p>
                <h2>ODS 11 e 13</h2><p>O sistema se relaciona aos objetivos de cidades resilientes e ação climática. Trata-se de um protótipo educacional, ainda sem validação de impacto social.</p>
                <h2>Compromisso com informação responsável</h2><p>O sistema não confirma eventos meteorológicos, não prediz desastres e não substitui Defesa Civil ou serviços de emergência. Os dados iniciais são fictícios.</p></section>
                """;
        return Html.layout("Sobre", corpo, "sobre");
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
