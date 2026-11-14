package br.com.planob.tests;

import br.com.planob.config.Database;
import br.com.planob.domain.AcaoRecuperacao;
import br.com.planob.domain.Localizacao;
import br.com.planob.domain.MudancaStatus;
import br.com.planob.domain.Ocorrencia;
import br.com.planob.domain.OcorrenciaFactory;
import br.com.planob.domain.StatusOcorrencia;
import br.com.planob.domain.TipoOcorrencia;
import br.com.planob.repository.DataAccessException;
import br.com.planob.repository.JdbcAcaoRepository;
import br.com.planob.repository.JdbcOcorrenciaRepository;
import br.com.planob.service.OcorrenciaService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Teste de integração contra PostgreSQL real (DB_URL, DB_USER, DB_PASSWORD).
 * Aplica database/schema.sql (idempotente) e valida CRUD, histórico transacional e integridade referencial.
 * Cada cenário usa dados próprios e os remove ao final.
 */
public final class JdbcIntegrationTests {
    private static final Path SCHEMA = Path.of("database", "schema.sql");
    private static final Path SEED = Path.of("database", "seed.sql");
    private static int verificacoes;

    private static Database banco;
    private static JdbcOcorrenciaRepository ocorrencias;
    private static JdbcAcaoRepository acoes;
    private static OcorrenciaService service;

    public static void main(String[] args) throws Exception {
        banco = Database.doAmbiente();
        if (!banco.disponivel()) throw new IllegalStateException("PostgreSQL indisponível; verifique DB_URL/DB_USER/DB_PASSWORD.");
        ocorrencias = new JdbcOcorrenciaRepository(banco);
        acoes = new JdbcAcaoRepository(banco);
        service = new OcorrenciaService(ocorrencias);

        executarScript(SCHEMA, true);
        executarScript(SCHEMA, true); // idempotência

        crudOcorrencia();
        filtrosEContagem();
        historicoTransacional();
        rollbackQuandoHistoricoFalha();
        acoesEComposicao();
        integridadeEConstraints();
        entradaMalignaNaoViraSql();
        seedValida();

        System.out.println("APROVADO: " + verificacoes + " verificações JDBC/PostgreSQL.");
    }

    private static void crudOcorrencia() {
        long id = service.cadastrar(TipoOcorrencia.VIA_OBSTRUIDA, "Teste Isolado", "Rua do Teste", "Registro de teste", 2);
        try {
            Ocorrencia lida = service.consultar(id);
            verificar(lida.severidade() == 2 && lida.status() == StatusOcorrencia.ABERTA, "INSERT + SELECT");
            verificar(lida.localizacao().bairro().equals("Teste Isolado"), "SELECT mapeia localização");

            service.atualizar(id, TipoOcorrencia.FALHA_ENERGIA, "Teste Isolado", "Rua Nova", "Atualizada", 4,
                    StatusOcorrencia.EM_ANALISE);
            Ocorrencia depois = service.consultar(id);
            verificar(depois.tipo() == TipoOcorrencia.FALHA_ENERGIA && depois.severidade() == 4
                    && depois.localizacao().logradouro().equals("Rua Nova")
                    && depois.criadaEm().equals(lida.criadaEm()), "UPDATE preserva criada_em");

            service.excluir(id);
            verificar(ocorrencias.buscar(id).isEmpty(), "DELETE");
            verificar(!ocorrencias.excluir(id), "DELETE repetido retorna false");
        } finally {
            ocorrencias.excluir(id);
        }
        Ocorrencia fantasma = OcorrenciaFactory.reconstituir(Long.MAX_VALUE, TipoOcorrencia.ALAGAMENTO,
                new Localizacao("X", "Y"), "inexistente", 1, StatusOcorrencia.ABERTA, Instant.now());
        verificar(!ocorrencias.atualizar(fantasma), "UPDATE de id inexistente retorna false");
    }

    private static void filtrosEContagem() {
        long a = service.cadastrar(TipoOcorrencia.ALAGAMENTO, "Filtro", "Rua A", "a", 1);
        long b = service.cadastrar(TipoOcorrencia.FALHA_ENERGIA, "Filtro", "Rua B", "b", 3);
        try {
            service.atualizar(b, TipoOcorrencia.FALHA_ENERGIA, "Filtro", "Rua B", "b", 3, StatusOcorrencia.EM_ANALISE);
            verificar(contem(ocorrencias.listar(StatusOcorrencia.ABERTA, TipoOcorrencia.ALAGAMENTO), a), "filtro status+tipo (a)");
            verificar(!contem(ocorrencias.listar(StatusOcorrencia.ABERTA, TipoOcorrencia.ALAGAMENTO), b), "filtro exclui outros");
            verificar(contem(ocorrencias.listar(StatusOcorrencia.EM_ANALISE, null), b), "filtro só status");
            verificar(contem(ocorrencias.listar(null, TipoOcorrencia.FALHA_ENERGIA), b), "filtro só tipo");
            verificar(contem(ocorrencias.listar(null, null), a) && contem(ocorrencias.listar(null, null), b), "sem filtros");
            Map<StatusOcorrencia, Long> contagem = ocorrencias.contarPorStatus();
            verificar(contagem.size() == StatusOcorrencia.values().length, "contagem inclui todos os status");
            verificar(contagem.get(StatusOcorrencia.EM_ANALISE) >= 1 && contagem.get(StatusOcorrencia.ABERTA) >= 1,
                    "contagem por status");
        } finally {
            ocorrencias.excluir(a);
            ocorrencias.excluir(b);
        }
    }

    private static void historicoTransacional() {
        long id = service.cadastrar(TipoOcorrencia.DANO_ESTRUTURAL, "Historico", "Rua H", "h", 5);
        try {
            service.atualizar(id, TipoOcorrencia.DANO_ESTRUTURAL, "Historico", "Rua H", "h", 5, StatusOcorrencia.EM_ANALISE);
            // Mesma situação com outro texto: não deve gerar novo histórico.
            service.atualizar(id, TipoOcorrencia.DANO_ESTRUTURAL, "Historico", "Rua H", "h2", 5, StatusOcorrencia.EM_ANALISE);
            service.atualizar(id, TipoOcorrencia.DANO_ESTRUTURAL, "Historico", "Rua H", "h2", 5, StatusOcorrencia.RESOLVIDA);
            List<MudancaStatus> h = ocorrencias.historico(id);
            verificar(h.size() == 3, "histórico: criação + 2 mudanças (sem duplicar)");
            verificar(h.get(0).anterior() == StatusOcorrencia.EM_ANALISE && h.get(0).atual() == StatusOcorrencia.RESOLVIDA,
                    "histórico mais recente primeiro");
            verificar(h.get(2).anterior() == null && h.get(2).atual() == StatusOcorrencia.ABERTA, "histórico de criação");
        } finally {
            ocorrencias.excluir(id);
        }
    }

    /** Força falha ao gravar o histórico e prova que o UPDATE da ocorrência também é desfeito. */
    private static void rollbackQuandoHistoricoFalha() throws Exception {
        long id = service.cadastrar(TipoOcorrencia.ALAGAMENTO, "Rollback", "Rua R", "r", 2);
        try {
            sql("CREATE OR REPLACE FUNCTION falha_historico_teste() RETURNS trigger AS $$ "
                    + "BEGIN RAISE EXCEPTION 'falha simulada'; END $$ LANGUAGE plpgsql");
            sql("CREATE TRIGGER trg_falha_historico_teste BEFORE INSERT ON historico_status "
                    + "FOR EACH ROW EXECUTE FUNCTION falha_historico_teste()");
            verificar(lanca(DataAccessException.class, () -> {
                service.atualizar(id, TipoOcorrencia.ALAGAMENTO, "Rollback", "Rua R", "r", 2, StatusOcorrencia.EM_ANALISE);
                return null;
            }), "falha no histórico propaga DataAccessException");
            verificar(service.consultar(id).status() == StatusOcorrencia.ABERTA, "rollback: status não mudou");
            verificar(ocorrencias.historico(id).size() == 1, "rollback: histórico intacto");

            Ocorrencia nova = OcorrenciaFactory.nova(TipoOcorrencia.ALAGAMENTO, new Localizacao("Rollback", "Rua R2"), "r2", 1);
            verificar(lanca(DataAccessException.class, () -> ocorrencias.criar(nova)), "criar falha sem histórico");
            verificar(ocorrencias.listar(null, null).stream().noneMatch(o -> o.localizacao().logradouro().equals("Rua R2")),
                    "rollback: ocorrência não foi criada pela metade");
        } finally {
            sql("DROP TRIGGER IF EXISTS trg_falha_historico_teste ON historico_status");
            sql("DROP FUNCTION IF EXISTS falha_historico_teste()");
            ocorrencias.excluir(id);
        }
    }

    private static void acoesEComposicao() {
        long id = service.cadastrar(TipoOcorrencia.VIA_OBSTRUIDA, "Acoes", "Rua Ac", "ac", 3);
        long outra = service.cadastrar(TipoOcorrencia.VIA_OBSTRUIDA, "Acoes", "Rua Ac2", "ac2", 3);
        try {
            long acaoId = acoes.criar(AcaoRecuperacao.nova(id, "Verificar situação", "Equipe de teste"));
            verificar(acoes.listarPorOcorrencia(id).size() == 1 && !acoes.listarPorOcorrencia(id).get(0).concluida(),
                    "INSERT + SELECT de ação");
            verificar(!acoes.concluir(acaoId, outra), "concluir exige a ocorrência correta");
            verificar(acoes.concluir(acaoId, id), "UPDATE concluir");
            verificar(acoes.listarPorOcorrencia(id).get(0).concluida(), "ação concluída persistida");
            verificar(!acoes.concluir(acaoId, id), "concluir repetido retorna false");
            verificar(acoes.listarPorOcorrencia(outra).isEmpty(), "ações isoladas por ocorrência");

            ocorrencias.excluir(id);
            verificar(contar("SELECT COUNT(*) FROM acoes_recuperacao WHERE ocorrencia_id = ?", id) == 0, "CASCADE remove ações");
            verificar(contar("SELECT COUNT(*) FROM historico_status WHERE ocorrencia_id = ?", id) == 0, "CASCADE remove histórico");
        } finally {
            ocorrencias.excluir(id);
            ocorrencias.excluir(outra);
        }
    }

    private static void integridadeEConstraints() {
        verificar(lanca(DataAccessException.class, () -> acoes.criar(AcaoRecuperacao.nova(Long.MAX_VALUE, "Órfã", "Ninguém"))),
                "FK recusa ação sem ocorrência");
        long id = service.cadastrar(TipoOcorrencia.ALAGAMENTO, "Constraints", "Rua C", "c", 1);
        try {
            verificar(rejeita("INSERT INTO ocorrencias (tipo,bairro,logradouro,descricao,severidade) VALUES ('ALAGAMENTO','b','l','d',9)"),
                    "CHECK severidade");
            verificar(rejeita("INSERT INTO ocorrencias (tipo,bairro,logradouro,descricao,severidade) VALUES ('INVALIDO','b','l','d',1)"),
                    "CHECK tipo");
            verificar(rejeita("INSERT INTO ocorrencias (tipo,bairro,logradouro,descricao,severidade,status) VALUES ('ALAGAMENTO','b','l','d',1,'X')"),
                    "CHECK status");
            verificar(rejeita("INSERT INTO ocorrencias (tipo,bairro,logradouro,descricao,severidade) VALUES ('ALAGAMENTO','  ','l','d',1)"),
                    "CHECK texto em branco");
            verificar(rejeita("INSERT INTO historico_status (ocorrencia_id,status_anterior,status_novo) VALUES (" + id + ",'ABERTA','ABERTA')"),
                    "CHECK histórico sem mudança");
            verificar(rejeita("INSERT INTO historico_status (ocorrencia_id,status_novo) VALUES (-5,'ABERTA')"),
                    "FK histórico");
            verificar(rejeita("UPDATE historico_status SET id = 1 WHERE ocorrencia_id = " + id),
                    "identity GENERATED ALWAYS protege o id");
        } finally {
            ocorrencias.excluir(id);
        }
    }

    private static void entradaMalignaNaoViraSql() {
        String perigoso = "x'); DROP TABLE ocorrencias; --";
        long id = service.cadastrar(TipoOcorrencia.ALAGAMENTO, "Injecao", perigoso, "d", 1);
        try {
            verificar(service.consultar(id).localizacao().logradouro().equals(perigoso), "texto malicioso gravado literalmente");
            verificar(contem(ocorrencias.listar(null, null), id), "tabela segue íntegra");
        } finally {
            ocorrencias.excluir(id);
        }
    }

    /** Aplica o seed dentro de uma transação, valida e desfaz: não polui o banco. */
    private static void seedValida() throws Exception {
        long antes = contar("SELECT COUNT(*) FROM ocorrencias");
        try (Connection c = banco.abrir()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                for (String comando : comandos(SEED)) st.execute(comando);
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ocorrencias")) {
                    rs.next();
                    verificar(rs.getLong(1) - antes == 4, "seed insere 4 ocorrências fictícias");
                }
                try (ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM ocorrencias o WHERE o.status <> (SELECT h.status_novo FROM historico_status h "
                                + "WHERE h.ocorrencia_id = o.id ORDER BY h.id DESC LIMIT 1)")) {
                    rs.next();
                    verificar(rs.getLong(1) == 0, "histórico do seed termina no status atual");
                }
            } finally {
                c.rollback();
            }
        }
        verificar(contar("SELECT COUNT(*) FROM ocorrencias") == antes, "seed validado e revertido");
    }

    // ---- utilitários ----

    private static boolean contem(List<Ocorrencia> lista, long id) {
        return lista.stream().anyMatch(o -> o.id() != null && o.id() == id);
    }

    private static boolean lanca(Class<? extends Throwable> tipo, Callable<?> acao) {
        try {
            acao.call();
            return false;
        } catch (Throwable t) {
            return tipo.isInstance(t);
        }
    }

    private static boolean rejeita(String comando) {
        try (Connection c = banco.abrir(); Statement st = c.createStatement()) {
            st.execute(comando);
            return false;
        } catch (SQLException e) {
            return true;
        }
    }

    private static void sql(String comando) throws SQLException {
        try (Connection c = banco.abrir(); Statement st = c.createStatement()) {
            st.execute(comando);
        }
    }

    private static long contar(String consulta, Object... params) {
        try (Connection c = banco.abrir(); PreparedStatement ps = c.prepareStatement(consulta)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void executarScript(Path arquivo, boolean obrigatorio) throws Exception {
        try (Connection c = banco.abrir(); Statement st = c.createStatement()) {
            for (String comando : comandos(arquivo)) st.execute(comando);
        }
        verificar(obrigatorio, "script " + arquivo + " aplicado");
    }

    private static List<String> comandos(Path arquivo) throws Exception {
        List<String> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            if (linha.strip().startsWith("--")) continue;
            sb.append(linha).append('\n');
        }
        for (String parte : sb.toString().split(";")) {
            if (!parte.isBlank()) lista.add(parte.strip());
        }
        return lista;
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError("FALHOU: " + mensagem);
        verificacoes++;
    }
}
