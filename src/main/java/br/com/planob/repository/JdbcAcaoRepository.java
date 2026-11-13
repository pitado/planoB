package br.com.planob.repository;

import br.com.planob.config.Database;
import br.com.planob.domain.AcaoRecuperacao;
import br.com.planob.domain.Validacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdbcAcaoRepository implements AcaoRepository {
    private final Database database;

    public JdbcAcaoRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public long criar(AcaoRecuperacao acao) {
        String sql = "INSERT INTO acoes_recuperacao (ocorrencia_id, titulo, responsavel) VALUES (?, ?, ?)";
        try (Connection connection = database.abrir();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, acao.ocorrenciaId());
            statement.setString(2, acao.titulo());
            statement.setString(3, acao.responsavel());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Banco não retornou o ID da ação.");
                return keys.getLong(1);
            }
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao adicionar ação de recuperação.", error);
        }
    }

    @Override
    public List<AcaoRecuperacao> listarPorOcorrencia(long ocorrenciaId) {
        Validacao.idPositivo(ocorrenciaId);
        String sql = "SELECT * FROM acoes_recuperacao WHERE ocorrencia_id=? ORDER BY concluida, criada_em, id";
        try (Connection connection = database.abrir(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ocorrenciaId);
            List<AcaoRecuperacao> acoes = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    acoes.add(new AcaoRecuperacao(result.getLong("id"), result.getLong("ocorrencia_id"),
                            result.getString("titulo"), result.getString("responsavel"),
                            result.getBoolean("concluida"), result.getTimestamp("criada_em").toInstant()));
                }
            }
            return acoes;
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao listar ações.", error);
        }
    }

    @Override
    public boolean concluir(long acaoId, long ocorrenciaId) {
        Validacao.idPositivo(acaoId);
        Validacao.idPositivo(ocorrenciaId);
        String sql = "UPDATE acoes_recuperacao SET concluida=TRUE WHERE id=? AND ocorrencia_id=? AND concluida=FALSE";
        try (Connection connection = database.abrir(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, acaoId);
            statement.setLong(2, ocorrenciaId);
            return statement.executeUpdate() > 0;
        } catch (SQLException error) {
            throw new DataAccessException("Erro ao concluir ação.", error);
        }
    }
}
