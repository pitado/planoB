package br.com.planob.repository;

import br.com.planob.config.Database;
import br.com.planob.domain.AcaoRecuperacao;
import br.com.planob.domain.Validacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Ações de recuperação em PostgreSQL; a chave estrangeira garante que toda ação pertence a uma ocorrência. */
public final class JdbcAcaoRepository implements AcaoRepository {
    private final Database database;

    public JdbcAcaoRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public long criar(AcaoRecuperacao acao) {
        Objects.requireNonNull(acao);
        String sql = "INSERT INTO acoes_recuperacao (ocorrencia_id, titulo, responsavel, concluida, criada_em) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = database.abrir(); PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
            ps.setLong(1, acao.ocorrenciaId());
            ps.setString(2, acao.titulo());
            ps.setString(3, acao.responsavel());
            ps.setBoolean(4, acao.concluida());
            ps.setObject(5, acao.criadaEm().atOffset(ZoneOffset.UTC));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("O banco não retornou o ID da ação.");
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao criar ação de recuperação.", e);
        }
    }

    @Override
    public List<AcaoRecuperacao> listarPorOcorrencia(long ocorrenciaId) {
        Validacao.idPositivo(ocorrenciaId);
        String sql = "SELECT id, ocorrencia_id, titulo, responsavel, concluida, criada_em FROM acoes_recuperacao "
                + "WHERE ocorrencia_id = ? ORDER BY concluida, criada_em, id";
        try (Connection c = database.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ocorrenciaId);
            List<AcaoRecuperacao> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new AcaoRecuperacao(rs.getLong("id"), rs.getLong("ocorrencia_id"),
                            rs.getString("titulo"), rs.getString("responsavel"), rs.getBoolean("concluida"),
                            rs.getObject("criada_em", OffsetDateTime.class).toInstant()));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao listar ações de recuperação.", e);
        }
    }

    @Override
    public boolean concluir(long acaoId, long ocorrenciaId) {
        Validacao.idPositivo(acaoId);
        Validacao.idPositivo(ocorrenciaId);
        String sql = "UPDATE acoes_recuperacao SET concluida = TRUE WHERE id = ? AND ocorrencia_id = ? AND concluida = FALSE";
        try (Connection c = database.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, acaoId);
            ps.setLong(2, ocorrenciaId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Erro ao concluir ação de recuperação.", e);
        }
    }
}
