package br.com.planob.web;

import br.com.planob.domain.Ocorrencia;
import br.com.planob.domain.StatusOcorrencia;
import br.com.planob.domain.TipoOcorrencia;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Renderização HTML com escape explícito dos dados fornecidos pelo usuário. */
public final class Html {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private Html() {}

    public static String escape(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String data(Instant instante) {
        return DATA.format(instante);
    }

    public static String layout(String titulo, String pagina, String menu) {
        return """
                <!doctype html><html lang="pt-BR"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s — Plano B</title><link rel="stylesheet" href="/assets/site.css"></head>
                <body><div class="layout"><aside class="sidebar">
                <a class="brand" href="/"><span class="brand-mark">B<span>.</span></span><span>PLANO B<small>Resiliência urbana</small></span></a>
                <nav class="nav"><a class="%s" href="/">Visão geral</a><a class="%s" href="/ocorrencias">Ocorrências</a><a class="%s" href="/sobre">Sobre o projeto</a></nav>
                <div class="sidebar-note">Protótipo acadêmico · Dados de demonstração.<br>Não é um serviço oficial de emergência.</div>
                </aside><div class="main"><header class="topbar"><span class="eyebrow">Maringá · Paraná</span><span class="tag">ODS 11 + ODS 13</span></header>
                <main class="content">%s</main><footer>PLANO B · Projeto acadêmico de resiliência urbana · 2026</footer></div></div></body></html>
                """.formatted(escape(titulo), menu.equals("inicio") ? "active" : "",
                menu.equals("ocorrencias") ? "active" : "", menu.equals("sobre") ? "active" : "", pagina);
    }

    public static String badge(StatusOcorrencia status) {
        return "<span class=\"badge " + status.name().toLowerCase() + "\">" + escape(status.descricao()) + "</span>";
    }

    public static String resumo(Ocorrencia o) {
        return "<tr><td><a class=\"table-link\" href=\"/ocorrencias/" + o.id() + "\">#" + o.id() + " · "
                + escape(o.tipo().descricao()) + "</a><small>" + escape(o.localizacao().logradouro()) + "</small></td>"
                + "<td>" + escape(o.localizacao().bairro()) + "</td><td>" + badge(o.status()) + "</td>"
                + "<td><strong>" + o.prioridade().descricao() + "</strong><small>Índice " + o.calcularIndiceImpacto() + "/100</small></td></tr>";
    }

    public static String tipos(TipoOcorrencia selecionado, boolean incluirTodos) {
        StringBuilder html = new StringBuilder();
        if (incluirTodos) html.append("<option value=\"\">Todos os tipos</option>");
        for (TipoOcorrencia tipo : TipoOcorrencia.values()) {
            html.append("<option value=\"").append(tipo.name()).append("\"")
                    .append(tipo == selecionado ? " selected" : "").append(">")
                    .append(escape(tipo.descricao())).append("</option>");
        }
        return html.toString();
    }

    public static String status(StatusOcorrencia selecionado, boolean incluirTodos) {
        StringBuilder html = new StringBuilder();
        if (incluirTodos) html.append("<option value=\"\">Todos os status</option>");
        for (StatusOcorrencia status : StatusOcorrencia.values()) {
            html.append("<option value=\"").append(status.name()).append("\"")
                    .append(status == selecionado ? " selected" : "").append(">")
                    .append(escape(status.descricao())).append("</option>");
        }
        return html.toString();
    }
}
