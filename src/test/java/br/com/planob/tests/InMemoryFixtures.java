package br.com.planob.tests;

import br.com.planob.domain.AcaoRecuperacao;
import br.com.planob.domain.MudancaStatus;
import br.com.planob.domain.Ocorrencia;
import br.com.planob.domain.OcorrenciaFactory;
import br.com.planob.domain.StatusOcorrencia;
import br.com.planob.domain.TipoOcorrencia;
import br.com.planob.repository.AcaoRepository;
import br.com.planob.repository.OcorrenciaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Dublês apenas para testes; o aplicativo de produção sempre usa JDBC. */
public final class InMemoryFixtures {
    private InMemoryFixtures() {}

    public static final class Ocorrencias implements OcorrenciaRepository {
        private long sequencia;
        private final Map<Long, Ocorrencia> dados = new LinkedHashMap<>();
        private final Map<Long, List<MudancaStatus>> historico = new LinkedHashMap<>();

        @Override
        public synchronized long criar(Ocorrencia registro) {
            long id = ++sequencia;
            dados.put(id, copiar(registro, id));
            historico.put(id, new ArrayList<>(List.of(new MudancaStatus(null, registro.status(), Instant.now()))));
            return id;
        }

        @Override
        public synchronized Optional<Ocorrencia> buscar(long id) {
            return Optional.ofNullable(dados.get(id));
        }

        @Override
        public synchronized List<Ocorrencia> listar(StatusOcorrencia status, TipoOcorrencia tipo) {
            return dados.values().stream().filter(o -> status == null || o.status() == status)
                    .filter(o -> tipo == null || o.tipo() == tipo).toList();
        }

        @Override
        public synchronized boolean atualizar(Ocorrencia registro) {
            Ocorrencia antigo = dados.get(registro.id());
            if (antigo == null) return false;
            dados.put(registro.id(), registro);
            if (antigo.status() != registro.status()) {
                historico.get(registro.id()).add(new MudancaStatus(antigo.status(), registro.status(), Instant.now()));
            }
            return true;
        }

        @Override
        public synchronized boolean excluir(long id) {
            historico.remove(id);
            return dados.remove(id) != null;
        }

        @Override
        public synchronized Map<StatusOcorrencia, Long> contarPorStatus() {
            Map<StatusOcorrencia, Long> mapa = new EnumMap<>(StatusOcorrencia.class);
            for (StatusOcorrencia s : StatusOcorrencia.values()) {
                mapa.put(s, dados.values().stream().filter(o -> o.status() == s).count());
            }
            return mapa;
        }

        @Override
        public synchronized List<MudancaStatus> historico(long id) {
            return List.copyOf(historico.getOrDefault(id, List.of()));
        }

        private static Ocorrencia copiar(Ocorrencia o, long id) {
            return OcorrenciaFactory.reconstituir(id, o.tipo(), o.localizacao(), o.descricao(),
                    o.severidade(), o.status(), o.criadaEm());
        }
    }

    public static final class Acoes implements AcaoRepository {
        private long sequencia;
        private final Map<Long, AcaoRecuperacao> dados = new LinkedHashMap<>();

        @Override
        public synchronized long criar(AcaoRecuperacao acao) {
            long id = ++sequencia;
            dados.put(id, new AcaoRecuperacao(id, acao.ocorrenciaId(), acao.titulo(),
                    acao.responsavel(), false, acao.criadaEm()));
            return id;
        }

        @Override
        public synchronized List<AcaoRecuperacao> listarPorOcorrencia(long ocorrenciaId) {
            return dados.values().stream().filter(a -> a.ocorrenciaId() == ocorrenciaId).toList();
        }

        @Override
        public synchronized boolean concluir(long acaoId, long ocorrenciaId) {
            AcaoRecuperacao acao = dados.get(acaoId);
            if (acao == null || acao.ocorrenciaId() != ocorrenciaId || acao.concluida()) return false;
            dados.put(acaoId, new AcaoRecuperacao(acao.id(), acao.ocorrenciaId(),
                    acao.titulo(), acao.responsavel(), true, acao.criadaEm()));
            return true;
        }
    }
}
