package br.com.planob.web;

import br.com.planob.domain.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Componentes HTML: dados externos sempre são escapados antes de inserir no documento. */
public final class Html {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private Html() {}

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String data(Instant instant) {
        return instant == null ? "—" : DATA.format(instant);
    }

    public static String layout(String titulo, String main, String active) {
        String inicio = active.equals("inicio") ? "active" : "";
        String ocorrencias = active.equals("ocorrencias") ? "active" : "";
        String sobre = active.equals("sobre") ? "active" : "";
        return """
            <!doctype html>
            <html lang="pt-BR">
            <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <meta name="theme-color" content="#294637">
            <title>%s | Plano B</title><link rel="stylesheet" href="/assets/site.css"></head>
            <body>
              <div class="shell">
                <header class="header">
                  <a class="logo" href="/" aria-label="Plano B - página inicial">
                    <span class="brandmark">B<span class="dot">.</span></span><span>Plano B<small>Organização comunitária</small></span>
                  </a>
                  <nav aria-label="Navegação principal">
                    <a class="%s" href="/">Visão geral</a>
                    <a class="%s" href="/ocorrencias">Ocorrências</a>
                    <a class="%s" href="/sobre">Sobre</a>
                  </nav>
                  <a class="btn dark desktop-new" href="/ocorrencias/nova">Nova ocorrência</a>
                </header>
                <main id="conteudo">%s</main>
                <footer>Plano B · Protótipo acadêmico com dados fictícios · Não é um serviço oficial de emergência.</footer>
              </div>
            </body>
            </html>
            """.formatted(esc(titulo), inicio, ocorrencias, sobre, main);
    }

    public static String badge(StatusOcorrencia s) {
        return "<span class=\"badge " + s.name().toLowerCase() + "\">" + esc(s.descricao()) + "</span>";
    }

    public static String badge(Prioridade p) {
        return "<span class=\"priority " + p.name().toLowerCase() + "\">" + esc(p.descricao()) + "</span>";
    }

    public static String tipos(TipoOcorrencia selecionado, boolean todos) {
        StringBuilder h = new StringBuilder();
        if(todos) h.append("<option value=\"\">Todos os tipos</option>");
        for(TipoOcorrencia t : TipoOcorrencia.values())
            h.append("<option value=\"").append(t.name()).append("\"")
            .append(t == selecionado ? " selected" : "").append(">")
            .append(esc(t.descricao())).append("</option>");
        return h.toString();
    }

    public static String status(StatusOcorrencia selecionado, boolean todos) {
        StringBuilder h = new StringBuilder();
        if(todos) h.append("<option value=\"\">Todos os estados</option>");
        for(StatusOcorrencia s : StatusOcorrencia.values())
            h.append("<option value=\"").append(s.name()).append("\"")
            .append(s == selecionado ? " selected" : "").append(">")
            .append(esc(s.descricao())).append("</option>");
        return h.toString();
    }

    public static String tabela(java.util.List<Ocorrencia> lista) {
        if(lista.isEmpty()) return "<div class=\"empty\"><strong>Nenhuma ocorrência encontrada.</strong><p>Registre um impacto ou revise os filtros da consulta.</p></div>";
        StringBuilder h = new StringBuilder("<div class=\"table-scroll\"><table><thead><tr><th>Ocorrência</th><th>Local</th><th>Situação</th><th>Prioridade</th></tr></thead><tbody>");
        for(Ocorrencia o : lista) {
            h.append("<tr><td><a class=\"item-link\" href=\"/ocorrencias/").append(o.id()).append("\">")
            .append(esc(o.tipo().descricao())).append("</a><small>#").append(o.id()).append(" · ")
            .append(esc(data(o.criadaEm()))).append("</small></td><td>")
            .append(esc(o.localizacao().bairro())).append("<small>")
            .append(esc(o.localizacao().logradouro())).append("</small></td><td>")
            .append(badge(o.status())).append("</td><td>").append(badge(o.prioridade()))
            .append("<small>Índice ").append(o.calcularIndiceImpacto()).append("/100</small></td></tr>");
        }
        return h.append("</tbody></table></div>").toString();
    }
}
