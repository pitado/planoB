package br.com.planob.web;

import br.com.planob.domain.*;
import br.com.planob.repository.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Dados voláteis para demonstrar a web SEM SQL. Não persistir informações reais. */
public final class DemoRepositories {
    private DemoRepositories(){}

    public static final class Ocorrencias implements OcorrenciaRepository {
        private long sequence;
        private final Map<Long,Ocorrencia> itens=new LinkedHashMap<>();
        private final Map<Long,List<MudancaStatus>> history=new LinkedHashMap<>();

        @Override public synchronized long criar(Ocorrencia o){
            long id=++sequence;
            itens.put(id,OcorrenciaFactory.reconstituir(id,o.tipo(),o.localizacao(),o.descricao(),o.severidade(),o.status(),o.criadaEm()));
            history.put(id,new ArrayList<>(List.of(new MudancaStatus(null,o.status(),Instant.now()))));
            return id;
        }
        @Override public synchronized Optional<Ocorrencia> buscar(long id){return Optional.ofNullable(itens.get(id));}
        @Override public synchronized List<Ocorrencia> listar(StatusOcorrencia status,TipoOcorrencia tipo){
            return itens.values().stream().filter(x->status==null||x.status()==status)
                .filter(x->tipo==null||x.tipo()==tipo).toList();
        }
        @Override public synchronized boolean atualizar(Ocorrencia o){
            Ocorrencia old=itens.get(o.id());
            if(old==null)return false;
            if(old.status()!=o.status())history.get(o.id()).add(new MudancaStatus(old.status(),o.status(),Instant.now()));
            itens.put(o.id(),o);
            return true;
        }
        @Override public synchronized boolean excluir(long id){
            history.remove(id);
            return itens.remove(id)!=null;
        }
        @Override public synchronized Map<StatusOcorrencia,Long> contarPorStatus(){
            Map<StatusOcorrencia,Long> result=new EnumMap<>(StatusOcorrencia.class);
            for(StatusOcorrencia s:StatusOcorrencia.values())result.put(s,itens.values().stream().filter(x->x.status()==s).count());
            return result;
        }
        @Override public synchronized List<MudancaStatus> historico(long id){
            return List.copyOf(history.getOrDefault(id,List.of()));
        }
    }

    public static final class Acoes implements AcaoRepository {
        private long sequence;
        private final Map<Long,AcaoRecuperacao> itens=new LinkedHashMap<>();
        @Override public synchronized long criar(AcaoRecuperacao a){
            long id=++sequence;
            itens.put(id,new AcaoRecuperacao(id,a.ocorrenciaId(),a.titulo(),a.responsavel(),false,a.criadaEm()));
            return id;
        }
        @Override public synchronized List<AcaoRecuperacao> listarPorOcorrencia(long id){
            return itens.values().stream().filter(a->a.ocorrenciaId()==id).toList();
        }
        @Override public synchronized boolean concluir(long acaoId,long ocorrenciaId){
            AcaoRecuperacao a=itens.get(acaoId);
            if(a==null||a.ocorrenciaId()!=ocorrenciaId||a.concluida())return false;
            itens.put(acaoId,new AcaoRecuperacao(a.id(),a.ocorrenciaId(),a.titulo(),a.responsavel(),true,a.criadaEm()));
            return true;
        }
    }
}
