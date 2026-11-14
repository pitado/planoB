package br.com.planob.web;

import br.com.planob.domain.*;
import br.com.planob.repository.AcaoRepository;
import br.com.planob.repository.OcorrenciaRepository;
import br.com.planob.service.OcorrenciaService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Páginas responsivas da Sprint 2. Não contém SQL nem conhece JDBC. */
public final class WebPages {
    private final OcorrenciaRepository ocorrencias;
    private final AcaoRepository acoes;
    private final OcorrenciaService service;

    public WebPages(OcorrenciaRepository ocorrencias, AcaoRepository acoes) {
        this.ocorrencias = Objects.requireNonNull(ocorrencias);
        this.acoes = Objects.requireNonNull(acoes);
        this.service = new OcorrenciaService(ocorrencias);
    }

    private static String heading(String eyebrow, String title, String subtitle, String actions) {
        return "<div class=\"page-head\"><div><div class=\"eyebrow\">" + Html.esc(eyebrow) + "</div><h1>"
            + Html.esc(title) + "</h1><p>" + Html.esc(subtitle) + "</p></div>"
            + (actions == null ? "" : "<div class=\"page-actions\">" + actions + "</div>") + "</div>";
    }

    public String painel() {
        Map<StatusOcorrencia,Long> counts = ocorrencias.contarPorStatus();
        List<Ocorrencia> todos = ocorrencias.listar(null, null).stream()
                .sorted(Comparator.comparing(Ocorrencia::criadaEm).reversed()).toList();
        String metrics = metric("Registros", todos.size(), "Total de ocorrências")
                + metric("Abertas", counts.getOrDefault(StatusOcorrencia.ABERTA, 0L), "Aguardando análise")
                + metric("Em recuperação", counts.getOrDefault(StatusOcorrencia.EM_RECUPERACAO, 0L), "Com ações em andamento")
                + metric("Resolvidas", counts.getOrDefault(StatusOcorrencia.RESOLVIDA, 0L), "Situação registrada");
        String main = """
            <section class="hero"><div class="eyebrow">RESILIÊNCIA URBANA</div>
              <h1>Organizar o agora.<br><span>Preparar o próximo passo.</span></h1>
              <p>Registre ocorrências, acompanhe o que mudou e organize ações de recuperação para a sua comunidade.</p>
              <div class="hero-actions"><a class="btn sand" href="/ocorrencias/nova">Registrar ocorrência</a>
                <a class="btn outline-light" href="/ocorrencias">Ver registros</a></div>
            </section>
            <section aria-label="Resumo de ocorrências" class="metrics">%s</section>
            <section class="surface"><div class="section-title"><h2>Registros recentes</h2><a href="/ocorrencias">Ver todos</a></div>%s</section>
            <p class="disclaimer">Ambiente demonstrativo. Os índices são ilustrativos e não representam avaliação oficial de risco.</p>
            """.formatted(metrics, Html.tabela(todos.subList(0, Math.min(6, todos.size()))));
        return Html.layout("Visão geral", main, "inicio");
    }

    private static String metric(String label, long value, String hint) {
        return "<div class=\"metric\"><span>" + Html.esc(label) + "</span><strong>" + value + "</strong><small>"
                + Html.esc(hint) + "</small></div>";
    }

    public String listagem(StatusOcorrencia status, TipoOcorrencia tipo) {
        List<Ocorrencia> lista = ocorrencias.listar(status,tipo).stream()
                .sorted(Comparator.comparing(Ocorrencia::criadaEm).reversed()).toList();
        String main = heading("Consulta", "Ocorrências", "Acompanhe cada situação e identifique os próximos passos.",
                "<a class=\"btn dark\" href=\"/ocorrencias/nova\">Registrar ocorrência</a>")
            + """
              <section class="surface">
                <form method="get" action="/ocorrencias" class="filters">
                  <label>Situação<select name="status">%s</select></label>
                  <label>Tipo<select name="tipo">%s</select></label>
                  <button class="btn dark" type="submit">Filtrar</button>
                  <a class="btn secondary" href="/ocorrencias">Limpar</a>
                </form>
                <div class="list-summary">%d registro(s) encontrado(s)</div>
                %s
              </section>
            """.formatted(Html.status(status,true), Html.tipos(tipo,true), lista.size(), Html.tabela(lista));
        return Html.layout("Ocorrências", main, "ocorrencias");
    }

    public String formulario(Ocorrencia o) {
        boolean editing = o != null;
        String action = editing ? "/ocorrencias/" + o.id() + "/atualizar" : "/ocorrencias";
        String title = editing ? "Editar ocorrência" : "Registrar ocorrência";
        String bairro = editing ? o.localizacao().bairro() : "";
        String logradouro = editing ? o.localizacao().logradouro() : "";
        String descricao = editing ? o.descricao() : "";
        int severidade = editing ? o.severidade() : 3;
        String statusControl = editing ? "<label>Situação<select name=\"status\">"
                + Html.statusPermitidos(o.status()) + "</select></label>"
                : "<p class=\"hint\">Uma nova ocorrência começa como <strong>Aberta</strong>.</p>";
        String main = heading("Registro de impacto",title,
                "Preencha informações fictícias para testar o fluxo do Plano B.",
                "<a class=\"btn secondary\" href=\"/ocorrencias\">Voltar à lista</a>")
            + """
              <section class="surface form-surface"><form method="post" action="%s">
                <div class="form-grid">
                  <label>Tipo da ocorrência<select name="tipo" required>%s</select></label>
                  <label>Severidade (1 a 5)<input name="severidade" type="number" min="1" max="5" required value="%d"></label>
                  <label>Bairro<input name="bairro" required maxlength="120" value="%s" placeholder="Ex.: Centro"></label>
                  <label>Rua / endereço<input name="logradouro" required maxlength="180" value="%s" placeholder="Ex.: Rua Exemplo, 100"></label>
                </div>
                <label>Descrição<textarea name="descricao" required minlength="3" maxlength="1200" rows="5" placeholder="Descreva o impacto e o que precisa ser acompanhado.">%s</textarea></label>
                %s
                <div class="form-actions"><button class="btn dark" type="submit">%s</button>
                <a class="btn secondary" href="/ocorrencias">Cancelar</a></div>
              </form></section>
              <p class="disclaimer">Não registre dados reais de outras pessoas; protótipo acadêmico sem autenticação.</p>
            """.formatted(action,Html.tipos(editing?o.tipo():TipoOcorrencia.VIA_OBSTRUIDA,false),
                severidade,Html.esc(bairro),Html.esc(logradouro),Html.esc(descricao),
                statusControl, editing?"Salvar alterações":"Cadastrar ocorrência");
        return Html.layout(title,main,"ocorrencias");
    }

    public String detalhe(long id) {
        Ocorrencia o = service.consultar(id);
        List<AcaoRecuperacao> tarefas = acoes.listarPorOcorrencia(id);
        StringBuilder actions = new StringBuilder();
        for(AcaoRecuperacao a : tarefas) {
            actions.append("<div class=\"task\"><div><strong>").append(Html.esc(a.titulo()))
                    .append("</strong><small>Responsável: ").append(Html.esc(a.responsavel())).append("</small></div>");
            if(a.concluida()) actions.append("<span class=\"completed\">Concluída</span>");
            else actions.append("<form method=\"post\" action=\"/ocorrencias/").append(id)
                    .append("/acoes/").append(a.id()).append("/concluir\">")
                    .append("<button class=\"btn secondary small\" type=\"submit\">Concluir</button></form>");
            actions.append("</div>");
        }
        if(tarefas.isEmpty()) actions.append("<p class=\"empty-inline\">Sem ações cadastradas. Adicione uma tarefa de recuperação abaixo.</p>");
        StringBuilder timeline = new StringBuilder();
        for(MudancaStatus h : ocorrencias.historico(id)) {
            timeline.append("<li><strong>").append(Html.esc(h.atual().descricao())).append("</strong><small>")
                    .append(Html.esc(Html.data(h.alteradaEm()))).append("</small></li>");
        }
        if(timeline.isEmpty()) timeline.append("<li><small>Nenhum histórico disponível.</small></li>");
        String main = "<a class=\"back\" href=\"/ocorrencias\">← Voltar às ocorrências</a>"
            + heading("Registro #"+id,o.tipo().descricao(),
                o.localizacao().bairro()+" · "+o.localizacao().logradouro(),
                "<a class=\"btn dark\" href=\"/ocorrencias/"+id+"/editar\">Editar</a><a class=\"btn danger\" href=\"/ocorrencias/"+id+"/excluir\">Excluir</a>")
            + """
              <div class="details-grid"><section class="surface">
                <div class="section-title"><h2>Informações do registro</h2>%s</div>
                <div class="facts"><div><small>Severidade</small><strong>%d / 5</strong></div>
                <div><small>Prioridade orientativa</small><strong>%s · %d/100</strong></div>
                <div><small>Registrada em</small><strong>%s</strong></div></div>
                <h3>Descrição</h3><p class="description">%s</p>
                <p class="hint">Índice ilustrativo: não indica emergência nem substitui órgãos oficiais.</p>
              </section>
              <section class="surface"><h2>Histórico de situação</h2><ol class="timeline">%s</ol></section></div>
              <section class="surface tasks-panel"><div class="section-title"><h2>Ações de recuperação</h2><span class="pill">%d tarefa(s)</span></div>
                %s
                <form method="post" action="/ocorrencias/%d/acoes" class="task-form">
                  <label>Ação<input name="titulo" maxlength="160" required placeholder="Próximo passo"></label>
                  <label>Responsável<input name="responsavel" maxlength="120" required placeholder="Equipe ou responsável"></label>
                  <button class="btn dark" type="submit">Adicionar ação</button>
                </form>
              </section>
            """.formatted(Html.badge(o.status()),o.severidade(),Html.esc(o.prioridade().descricao()),
                 o.calcularIndiceImpacto(),Html.esc(Html.data(o.criadaEm())),Html.esc(o.descricao()),
                 timeline,tarefas.size(),actions,id);
        return Html.layout("Ocorrência #"+id,main,"ocorrencias");
    }

    public String confirmar(long id) {
        Ocorrencia o = service.consultar(id);
        String main=heading("Confirmação","Excluir ocorrência","Essa operação removerá o registro e pode eliminar ações e histórico associados.",
                "<a class=\"btn secondary\" href=\"/ocorrencias/"+id+"\">Cancelar</a>")
            + "<section class=\"surface confirm\"><h2>"+Html.esc(o.tipo().descricao())+" · #"+id+"</h2><p>"
            +Html.esc(o.localizacao().bairro()+" — "+o.localizacao().logradouro())+"</p>"
            +"<form method=\"post\" action=\"/ocorrencias/"+id+"/excluir\">"
            +"<label class=\"check\"><input type=\"checkbox\" name=\"confirmar\" value=\"sim\" required> Entendo que esta ação não pode ser desfeita.</label>"
            +"<button class=\"btn danger\" type=\"submit\">Sim, excluir definitivamente</button></form></section>";
        return Html.layout("Confirmar exclusão",main,"ocorrencias");
    }

    public String sobre() {
        return Html.layout("Sobre","""
            <section class="surface about"><div class="eyebrow">SOBRE O PROJETO</div>
            <h1>Plano B é sobre organização quando a rotina muda.</h1>
            <p>Após temporais, alagamentos, quedas de energia e danos na cidade, a recuperação exige registrar situações e coordenar ações.</p>
            <p>Este protótipo reúne ocorrências, filtros, índice ilustrativo de prioridade, histórico de status e tarefas de recuperação.</p>
            <h2>Limites de uso</h2><p>Os registros são fictícios. O sistema não emite alertas, não verifica relatos reais e não substitui Defesa Civil ou serviços de emergência.</p>
            <a class="btn dark" href="/ocorrencias">Explorar ocorrências</a></section>
            ""","sobre");
    }

    public String erro(int code,String message) {
        return Html.layout("Aviso","<section class=\"surface error-page\"><div class=\"eyebrow\">"+code+"</div><h1>"
            +Html.esc(message)+"</h1><a class=\"btn dark\" href=\"/\">Voltar ao início</a></section>","");
    }
}
