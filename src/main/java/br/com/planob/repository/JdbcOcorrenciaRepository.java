package br.com.planob.repository;

import br.com.planob.config.Database;
import br.com.planob.domain.Localizacao;
import br.com.planob.domain.MudancaStatus;
import br.com.planob.domain.Ocorrencia;
import br.com.planob.domain.OcorrenciaFactory;
import br.com.planob.domain.StatusOcorrencia;
import br.com.planob.domain.TipoOcorrencia;
import br.com.planob.domain.Validacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Persistência JDBC: SQL sempre parametrizado; criação e mudança de status gravam o histórico na mesma transação. */
public final class JdbcOcorrenciaRepository implements OcorrenciaRepository {
    private static final int LIMITE_LISTAGEM = 200;
    private static final String COLUNAS = "id, tipo, bairro, logradouro, descricao, severidade, status, criada_em";

    private final Database database;

    public JdbcOcorrenciaRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public long criar(Ocorrencia o) {
        Objects.requireNonNull(o);
        String sql = "INSERT INTO ocorrencias (tipo, bairro, logradouro, descricao, severidade, status, criada_em) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = database.abrir()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
                ps.setString(1, o.tipo().name());
                ps.setString(2, o.localizacao().bairro());
                ps.setString(3, o.localizacao().logradouro());
                ps.setString(4, o.descricao());
                ps.setInt(5, o.severidade());
                ps.setString(6, o.status().name());
                ps.setObject(7, o.criadaEm().atOffset(ZoneOffset.UTC));
                ps.executeUpdate();
                long id;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("O banco não retornou o ID da ocorrência.");
                    id = keys.getLong(1);
                }
                registrarHistorico(c, id, null, o.status());
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao criar ocorrência.", e);
        }
    }

    @Override
    public Optional<Ocorrencia> buscar(long id) {
        Validacao.idPositivo(id);
        try (Connection c = database.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUNAS + " FROM ocorrencias WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao consultar ocorrência.", e);
        }
    }

    @Override
    public List<Ocorrencia> listar(StatusOcorrencia status, TipoOcorrencia tipo) {
        // Apenas fragmentos fixos são concatenados; os valores sempre vão por parâmetro.
        String sql = "SELECT " + COLUNAS + " FROM ocorrencias WHERE (?::varchar IS NULL OR status = ?) "
                + "AND (?::varchar IS NULL OR tipo = ?) ORDER BY criada_em DESC, id DESC LIMIT " + LIMITE_LISTAGEM;
        String s = status == null ? null : status.name();
        String t = tipo == null ? null : tipo.name();
        try (Connection c = database.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s);
            ps.setString(2, s);
            ps.setString(3, t);
            ps.setString(4, t);
            List<Ocorrencia> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao listar ocorrências.", e);
        }
    }

    @Override
    public boolean atualizar(Ocorrencia o) {
        Objects.requireNonNull(o);
        if (o.id() == null) throw new IllegalArgumentException("Ocorrência sem ID não pode ser atualizada.");
        try (Connection c = database.abrir()) {
            c.setAutoCommit(false);
            try {
                StatusOcorrencia anterior = travarEObterStatus(c, o.id());
                if (anterior == null) {
                    c.rollback();
                    return false;
                }
                String sql = "UPDATE ocorrencias SET tipo=?, bairro=?, logradouro=?, descricao=?, severidade=?, status=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, o.tipo().name());
                    ps.setString(2, o.localizacao().bairro());
                    ps.setString(3, o.localizacao().logradouro());
                    ps.setString(4, o.descricao());
                    ps.setInt(5, o.severidade());
                    ps.setString(6, o.status().name());
                    ps.setLong(7, o.id());
                    ps.executeUpdate();
                }
                if (anterior != o.status()) registrarHistorico(c, o.id(), anterior, o.status());
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao atualizar ocorrência.", e);
        }
    }

    @Override
    public boolean excluir(long id) {
        Validacao.idPositivo(id);
        try (Connection c = database.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM ocorrencias WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0; // ações e histórico saem por ON DELETE CASCADE
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao excluir ocorrência.", e);
        }
    }

    @Override
    public Map<StatusOcorrencia, Long> contarPorStatus() {
        Map<StatusOcorrencia, Long> mapa = new EnumMap<>(StatusOcorrencia.class);
        for (StatusOcorrencia s : StatusOcorrencia.values()) mapa.put(s, 0L);
        try (Connection c = database.abrir(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT status, COUNT(*) AS total FROM ocorrencias GROUP BY status")) {
            while (rs.next()) mapa.put(StatusOcorrencia.valueOf(rs.getString("status")), rs.getLong("total"));
            return mapa;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao contar ocorrências por status.", e);
        }
    }

    @Override
    public List<MudancaStatus> historico(long ocorrenciaId) {
        Validacao.idPositivo(ocorrenciaId);
        String sql = "SELECT status_anterior, status_novo, alterada_em FROM historico_status "
                + "WHERE ocorrencia_id = ? ORDER BY alterada_em DESC, id DESC";
        try (Connection c = database.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ocorrenciaId);
            List<MudancaStatus> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ant = rs.getString("status_anterior");
                    lista.add(new MudancaStatus(ant == null ? null : StatusOcorrencia.valueOf(ant),
                            StatusOcorrencia.valueOf(rs.getString("status_novo")),
                            rs.getObject("alterada_em", OffsetDateTime.class).toInstant()));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao consultar histórico.", e);
        }
    }

    private static StatusOcorrencia travarEObterStatus(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT status FROM ocorrencias WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? StatusOcorrencia.valueOf(rs.getString(1)) : null;
            }
        }
    }

    private static void registrarHistorico(Connection c, long id, StatusOcorrencia anterior, StatusOcorrencia novo)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO historico_status (ocorrencia_id, status_anterior, status_novo) VALUES (?, ?, ?)")) {
            ps.setLong(1, id);
            ps.setString(2, anterior == null ? null : anterior.name());
            ps.setString(3, novo.name());
            ps.executeUpdate();
        }
    }

    private static Ocorrencia mapear(ResultSet rs) throws SQLException {
        return OcorrenciaFactory.reconstituir(rs.getLong("id"),
                TipoOcorrencia.valueOf(rs.getString("tipo")),
                new Localizacao(rs.getString("bairro"), rs.getString("logradouro")),
                rs.getString("descricao"), rs.getInt("severidade"),
                StatusOcorrencia.valueOf(rs.getString("status")),
                rs.getObject("criada_em", OffsetDateTime.class).toInstant());
    }
}
