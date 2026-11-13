package br.com.planob.tests;

import br.com.planob.domain.*;
import br.com.planob.service.OcorrenciaService;
import java.util.NoSuchElementException;

/** Testes de regras e de casos de uso, executados com `java` sem JUnit ou banco. */
public final class DomainTests {
    private static int verificacoes;

    public static void main(String[] args) {
        tiposUsamHerancaEPolimorfismo();
        prioridadesESeveridade();
        validacaoDeCampos();
        crudDoServico();
        historicoDasTransicoes();
        acaoLigadaAOcorrencia();
        System.out.println("APROVADO: " + verificacoes + " verificações de domínio, CRUD e apresentação.");
    }

    private static void tiposUsamHerancaEPolimorfismo() {
        int[] esperados = {65, 60, 55, 70}; // severidade 3, pesos por subtipo
        int i = 0;
        for (TipoOcorrencia tipo : TipoOcorrencia.values()) {
            Ocorrencia o = OcorrenciaFactory.nova(tipo, new Localizacao("Zona 3", "Rua Modelo"), "Cenário fictício", 3);
            verificar(o.calcularIndiceImpacto() == esperados[i++], "@Override aplica peso do subtipo " + tipo);
            verificar(o instanceof Ocorrencia, "Subtipo herda de Ocorrencia");
        }
    }

    private static void prioridadesESeveridade() {
        Ocorrencia critica = OcorrenciaFactory.nova(TipoOcorrencia.DANO_ESTRUTURAL,
                new Localizacao("Centro", "Rua Fictícia"), "Exemplo", 5);
        verificar(critica.calcularIndiceImpacto() == 100, "Índice respeita limite máximo");
        verificar(critica.prioridade() == Prioridade.CRITICA, "Faixa de prioridade crítica");
        verificar(Prioridade.doIndice(39) == Prioridade.BAIXA, "Limite inferior");
        verificar(Prioridade.doIndice(40) == Prioridade.MEDIA, "Faixa média");
        verificar(Prioridade.doIndice(65) == Prioridade.ALTA, "Faixa alta");
    }

    private static void validacaoDeCampos() {
        falha(() -> new Localizacao(" ", "Rua"), "Rejeita bairro vazio");
        falha(() -> new Localizacao("Bairro", " "), "Rejeita logradouro vazio");
        falha(() -> OcorrenciaFactory.nova(TipoOcorrencia.ALAGAMENTO,
                new Localizacao("Zona", "Rua"), "Descricao", 0), "Rejeita severidade 0");
        falha(() -> OcorrenciaFactory.nova(TipoOcorrencia.ALAGAMENTO,
                new Localizacao("Zona", "Rua"), "Descricao", 6), "Rejeita severidade 6");
        falha(() -> AcaoRecuperacao.nova(1, "", "Equipe"), "Rejeita ação sem título");
        falha(() -> AcaoRecuperacao.nova(-1, "Tarefa", "Equipe"), "Rejeita ação sem ocorrência");
    }

    private static void crudDoServico() {
        InMemoryFixtures.Ocorrencias repo = new InMemoryFixtures.Ocorrencias();
        OcorrenciaService service = new OcorrenciaService(repo);
        long id = service.cadastrar(TipoOcorrencia.FALHA_ENERGIA, "Zona 3", "Rua Exemplo", "Falha fictícia", 2);
        verificar(id > 0, "Create devolve identificador");
        verificar(service.consultar(id).tipo() == TipoOcorrencia.FALHA_ENERGIA, "Read consulta entidade principal");
        verificar(repo.listar(null, TipoOcorrencia.FALHA_ENERGIA).size() == 1, "Filtros aplicados");
        service.atualizar(id, TipoOcorrencia.VIA_OBSTRUIDA, "Centro", "Rua Outra", "Editada", 4,
                StatusOcorrencia.EM_ANALISE);
        verificar(service.consultar(id).tipo() == TipoOcorrencia.VIA_OBSTRUIDA, "Update altera tipo");
        verificar(service.consultar(id).severidade() == 4, "Update altera severidade");
        verificar(service.consultar(id).status() == StatusOcorrencia.EM_ANALISE, "Update altera situação");
        service.excluir(id);
        verificar(repo.listar(null, null).isEmpty(), "Delete exclui ocorrência");
        try {
            service.consultar(id);
            throw new AssertionError("Read deveria falhar após Delete");
        } catch (NoSuchElementException esperado) {
            verificacoes++;
        }
    }

    private static void historicoDasTransicoes() {
        InMemoryFixtures.Ocorrencias repo = new InMemoryFixtures.Ocorrencias();
        OcorrenciaService service = new OcorrenciaService(repo);
        long id = service.cadastrar(TipoOcorrencia.ALAGAMENTO, "Bairro", "Rua", "Exemplo", 2);
        service.atualizar(id, TipoOcorrencia.ALAGAMENTO, "Bairro", "Rua", "Exemplo", 2,
                StatusOcorrencia.EM_RECUPERACAO);
        verificar(repo.historico(id).size() == 2, "Registra abertura e transição");
        verificar(repo.historico(id).get(1).anterior() == StatusOcorrencia.ABERTA, "Guarda status anterior");
    }

    private static void acaoLigadaAOcorrencia() {
        InMemoryFixtures.Acoes repo = new InMemoryFixtures.Acoes();
        long id = repo.criar(AcaoRecuperacao.nova(7, "Acionar canal oficial", "Equipe local"));
        verificar(repo.listarPorOcorrencia(7).size() == 1, "Ação ligada por ID");
        verificar(repo.concluir(id, 7), "Ação pode ser concluída");
        verificar(!repo.concluir(id, 7), "Ação concluída não é concluída duas vezes");
        verificar(!repo.concluir(id, 8), "Outra ocorrência não altera ação");
    }

    private static void falha(Runnable caso, String mensagem) {
        try { caso.run(); throw new AssertionError(mensagem); }
        catch (IllegalArgumentException esperado) { verificacoes++; }
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
        verificacoes++;
    }
}
