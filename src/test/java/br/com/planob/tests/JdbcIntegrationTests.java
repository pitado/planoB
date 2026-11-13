package br.com.planob.tests;

import br.com.planob.config.Database;
import br.com.planob.domain.AcaoRecuperacao;
import br.com.planob.domain.StatusOcorrencia;
import br.com.planob.domain.TipoOcorrencia;
import br.com.planob.repository.JdbcAcaoRepository;
import br.com.planob.repository.JdbcOcorrenciaRepository;
import br.com.planob.service.OcorrenciaService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Exige PostgreSQL com schema.sql aplicado e driver JDBC no classpath. */
public final class JdbcIntegrationTests {
    private static int verificacoes;

    public static void main(String[] args) throws Exception {
        Database banco = Database.doAmbiente();
        JdbcOcorrenciaRepository ocorrencias = new JdbcOcorrenciaRepository(banco);
        JdbcAcaoRepository acoes = new JdbcAcaoRepository(banco);
        OcorrenciaService service = new OcorrenciaService(ocorrencias);
        long id = service.cadastrar(TipoOcorrencia.VIA_OBSTRUIDA, "Teste Isolado", "Rua do Teste",
                "Registro gerado por testes automatizados", 2);
        try {
            verificar(service.consultar(id).severidade() == 2, "CREATE + READ JDBC");
            service.atualizar(id, TipoOcorrencia.FALHA_ENERGIA, "Teste Isolado", "Rua do Teste",
                    "Atualizado por teste", 4, StatusOcorrencia.EM_ANALISE);
            verificar(service.consultar(id).tipo() == TipoOcorrencia.FALHA_ENERGIA, "UPDATE JDBC");
            verificar(ocorrencias.historico(id).size() == 2, "Histórico transacional JDBC");
            verificar(ocorrencias.listar(StatusOcorrencia.EM_ANALISE, TipoOcorrencia.FALHA_ENERGIA)
                    .stream().anyMatch(o -> o.id() == id), "Filtro real por status e tipo");
            long acaoId = acoes.criar(AcaoRecuperacao.nova(id, "Verificar a situação", "Equipe de teste"));
            verificar(acoes.listarPorOcorrencia(id).size() == 1, "Composição 1:N em PostgreSQL");
            verificar(acoes.concluir(acaoId, id), "Conclusão persistida");
            verificar(acoes.listarPorOcorrencia(id).get(0).concluida(), "READ de ação concluída");
            service.excluir(id);
            verificar(ocorrencias.buscar(id).isEmpty(), "DELETE JDBC");
            try (Connection conexao = banco.abrir(); PreparedStatement p = conexao.prepareStatement(
                    "SELECT COUNT(*) FROM acoes_recuperacao WHERE ocorrencia_id=?")) {
                p.setLong(1, id);
                try (ResultSet resultado = p.executeQuery()) {
                    resultado.next();
                    verificar(resultado.getLong(1) == 0, "ON DELETE CASCADE elimina ações");
                }
            }
        } finally {
            // Limpeza mesmo em caso de teste interrompido.
            ocorrencias.excluir(id);
        }
        System.out.println("APROVADO: " + verificacoes + " verificações JDBC/PostgreSQL.");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
        verificacoes++;
    }
}
