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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Persistência real com JDBC, PreparedStatement e transações. */
public final class JdbcOcorrenciaRepository implements OcorrenciaRepository {
    private final Database database;

    public JdbcOcorrenciaRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public long criar(Ocorrencia ocorrencia) {
        Objects.requireNonNull(ocorrencia);
        String sql = "INSERT INTO ocorrencias (tipo, bairro, logradouro, descricao, severidade, status, criada_em) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.abrir()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preencher(statement, ocorrencia);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Banco não retornou o ID da ocorrência.");
                    long id = keys.getLong(1);
                    registrarHistorico(connection, id, null, ocorrencia.status());
                    connection.commit();
                    return id;
                }
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao criar ocorrência.", error);
        }
    }

    @Override
    public Optional<Ocorrencia> buscar(long id) {
        Validacao.idPositivo(id);
        String sql = "SELECT * FROM ocorrencias WHERE id = ?";
        try (Connection connection = database.abrir(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao consultar ocorrência.", error);
        }
    }

    @Override
    public List<Ocorrencia> listar(StatusOcorrencia status, TipoOcorrencia tipo) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ocorrencias WHERE 1=1");
        if (status != null) sql.append(" AND status = ?");
        if (tipo != null) sql.append(" AND tipo = ?");
        sql.append(" ORDER BY criada_em DESC, id DESC LIMIT 200");
        try (Connection connection = database.abrir(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int pos = 1;
            if (status != null) statement.setString(pos++, status.name());
            if (tipo != null) statement.setString(pos, tipo.name());
            List<Ocorrencia> ocorrencias = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) ocorrencias.add(mapear(result));
            }
            return ocorrencias;
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao listar ocorrências.", error);
        }
    }

    @Override
    public boolean atualizar(Ocorrencia ocorrencia) {
        Objects.requireNonNull(ocorrencia);
        if (ocorrencia.id() == null) throw new IllegalArgumentException("Ocorrência sem ID.");
        String sql = "UPDATE ocorrencias SET tipo=?, bairro=?, logradouro=?, descricao=?, severidade=?, status=? WHERE id=?";
        try (Connection connection = database.abrir()) {
            connection.setAutoCommit(false);
            try {
                StatusOcorrencia anterior;
                try (PreparedStatement consulta = connection.prepareStatement("SELECT status FROM ocorrencias WHERE id=? FOR UPDATE")) {
                    consulta.setLong(1, ocorrencia.id());
                    try (ResultSet result = consulta.executeQuery()) {
                        if (!result.next()) { connection.rollback(); return false; }
                        anterior = StatusOcorrencia.valueOf(result.getString(1));
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, ocorrencia.tipo().name());
                    statement.setString(2, ocorrencia.localizacao().bairro());
                    statement.setString(3, ocorrencia.localizacao().logradouro());
                    statement.setString(4, ocorrencia.descricao());
                    statement.setInt(5, ocorrencia.severidade());
                    statement.setString(6, ocorrencia.status().name());
                    statement.setLong(7, ocorrencia.id());
                    statement.executeUpdate();
                }
                if (anterior != ocorrencia.status()) {
                    registrarHistorico(connection, ocorrencia.id(), anterior, ocorrencia.status());
                }
                connection.commit();
                return true;
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao atualizar ocorrência.", error);
        }
    }

    @Override
    public boolean excluir(long id) {
        Validacao.idPositivo(id);
        try (Connection connection = database.abrir();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM ocorrencias WHERE id=?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao excluir ocorrência.", error);
        }
    }

    @Override
    public Map<StatusOcorrencia, Long> contarPorStatus() {
        EnumMap<StatusOcorrencia, Long> contagens = new EnumMap<>(StatusOcorrencia.class);
        for (StatusOcorrencia status : StatusOcorrencia.values()) contagens.put(status, 0L);
        try (Connection connection = database.abrir(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT status, COUNT(*) total FROM ocorrencias GROUP BY status")) {
            while (result.next()) {
                contagens.put(StatusOcorrencia.valueOf(result.getString("status")), result.getLong("total"));
            }
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao consultar indicadores.", error);
        }
        return contagens;
    }

    @Override
    public List<MudancaStatus> historico(long ocorrenciaId) {
        Validacao.idPositivo(ocorrenciaId);
        String sql = "SELECT status_anterior, status_novo, alterada_em FROM historico_status "
                   + "WHERE ocorrencia_id=? ORDER BY alterada_em DESC, id DESC";
        try (Connection connection = database.abrir(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ocorrenciaId);
            List<MudancaStatus> historico = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String anterior = result.getString("status_anterior");
                    historico.add(new MudancaStatus(anterior == null ? null : StatusOcorrencia.valueOf(anterior),
                            StatusOcorrencia.valueOf(result.getString("status_novo")), result.getTimestamp("alterada_em").toInstant()));
                }
            }
            return historico;
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao consultar histórico.", error);
        }
    }

    private static void registrarHistorico(Connection connection, long id, StatusOcorrencia anterior,
                                           StatusOcorrencia atual) throws SQLException {
        String sql = "INSERT INTO historico_status (ocorrencia_id, status_anterior, status_novo) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, anterior == null ? null : anterior.name());
            statement.setString(3, atual.name());
            statement.executeUpdate();
        }
    }

    private static void preencher(PreparedStatement statement, Ocorrencia o) throws SQLException {
        statement.setString(1, o.tipo().name());
        statement.setString(2, o.localizacao().bairro());
        statement.setString(3, o.localizacao().logradouro());
        statement.setString(4, o.descricao());
        statement.setInt(5, o.severidade());
        statement.setString(6, o.status().name());
        statement.setTimestamp(7, Timestamp.from(o.criadaEm()));
    }

    private static Ocorrencia mapear(ResultSet result) throws SQLException {
        return OcorrenciaFactory.reconstituir(result.getLong("id"),
                TipoOcorrencia.valueOf(result.getString("tipo")),
                new Localizacao(result.getString("bairro"), result.getString("logradouro")),
                result.getString("descricao"), result.getInt("severidade"),
                StatusOcorrencia.valueOf(result.getString("status")), result.getTimestamp("criada_em").toInstant());
    }
}
