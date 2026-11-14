package br.com.planob.tests;

import br.com.planob.config.Database;
import br.com.planob.repository.JdbcAcaoRepository;
import br.com.planob.repository.JdbcOcorrenciaRepository;
import br.com.planob.web.WebApp;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/** Teste ponta a ponta da integração: formulários HTTP + JDBC + PostgreSQL real. */
public final class WebDbTests {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private static int checks;
    private WebDbTests() {}

    private static HttpResponse<String> call(String base, String method, String path, String form) throws Exception {
        HttpRequest.Builder request=HttpRequest.newBuilder(URI.create(base+path));
        if("POST".equals(method)){
            request.header("Content-Type","application/x-www-form-urlencoded; charset=utf-8")
                   .POST(HttpRequest.BodyPublishers.ofString(form,StandardCharsets.UTF_8));
        } else request.GET();
        return CLIENT.send(request.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private static void check(boolean value,String description){
        if(!value)throw new AssertionError(description);
        checks++;
    }
    private static HttpResponse<String> expect(String base,String method,String path,String form,int status) throws Exception {
        var result=call(base,method,path,form);
        check(result.statusCode()==status,method+" "+path+" retornou "+result.statusCode()+", esperado "+status);
        return result;
    }
    private static String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private static long total(Database db,String table,long id) throws Exception {
        // table é uma constante fixa, nunca vem de entrada do usuário.
        try(var connection=db.abrir(); PreparedStatement stmt=connection.prepareStatement(
                "SELECT COUNT(*) FROM "+table+" WHERE ocorrencia_id = ?")){
            stmt.setLong(1,id);
            try(ResultSet result=stmt.executeQuery()){
                result.next();
                return result.getLong(1);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        Database db=Database.doAmbiente();
        check(db.disponivel(),"PostgreSQL deve estar acessível");
        var ocorrencias=new JdbcOcorrenciaRepository(db);
        var acoes=new JdbcAcaoRepository(db);
        String identificador="E2E-"+UUID.randomUUID().toString().substring(0,8);
        WebApp web=new WebApp(ocorrencias,acoes);
        long id=0;
        try {
            String base="http://127.0.0.1:"+web.iniciar(0);
            expect(base,"GET","/health","",200);
            expect(base,"GET","/","",200);
            var created=expect(base,"POST","/ocorrencias",
                    "tipo=ALAGAMENTO&bairro="+enc(identificador)+"&logradouro="+enc("Rua de teste")
                    +"&descricao="+enc("Teste integrado via HTTP e banco")+"&severidade=4",303);
            String path=created.headers().firstValue("Location").orElseThrow();
            check(path.matches("/ocorrencias/[0-9]+"),"Location aponta para a ocorrência criada");
            id=Long.parseLong(path.substring(path.lastIndexOf('/')+1));
            check(ocorrencias.buscar(id).isPresent(),"CREATE foi persistido");
            var detail=expect(base,"GET",path,"",200);
            check(detail.body().contains(identificador),"READ da ocorrência via HTML");

            expect(base,"POST",path+"/atualizar",
                    "tipo=ALAGAMENTO&bairro="+enc(identificador)+"&logradouro="+enc("Rua atualizada")
                    +"&descricao="+enc("Mudança persistida")+"&severidade=3&status=EM_ANALISE",303);
            check(ocorrencias.buscar(id).orElseThrow().localizacao().logradouro().equals("Rua atualizada"),
                    "UPDATE alterou registro no PostgreSQL");
            check(ocorrencias.historico(id).size()==2,"Histórico inclui criação e alteração de status");

            expect(base,"POST",path+"/acoes","titulo="+enc("Organizar mutirão")
                    +"&responsavel="+enc("Equipe teste"),303);
            var taskList=acoes.listarPorOcorrencia(id);
            check(taskList.size()==1,"Ação de recuperação persistiu");
            long acaoId=taskList.get(0).id();
            expect(base,"POST",path+"/acoes/"+acaoId+"/concluir","",303);
            check(acoes.listarPorOcorrencia(id).get(0).concluida(),"Tarefa concluída no banco");

            web.parar();
            web=new WebApp(new JdbcOcorrenciaRepository(db),new JdbcAcaoRepository(db));
            base="http://127.0.0.1:"+web.iniciar(0);
            detail=expect(base,"GET",path,"",200);
            check(detail.body().contains("Mudança persistida"),"READ após reiniciar servidor mantém alteração");
            check(detail.body().contains("Organizar mutirão"),"Ações mantidas após reiniciar");
            check(detail.body().contains("Histórico de situação"),"Histórico exibido após reiniciar");
            var filtered=expect(base,"GET","/ocorrencias?status=EM_ANALISE&tipo=ALAGAMENTO","",200);
            check(filtered.body().contains(identificador),"Filtros HTTP consultam banco real");

            var confirm=expect(base,"GET",path+"/excluir","",200);
            check(confirm.body().contains("Sim, excluir definitivamente"),"Exclusão exige confirmação");
            expect(base,"POST",path+"/excluir","confirmar=sim",303);
            check(ocorrencias.buscar(id).isEmpty(),"DELETE remove registro do banco");
            check(total(db,"acoes_recuperacao",id)==0,"ON DELETE CASCADE remove ações");
            check(total(db,"historico_status",id)==0,"ON DELETE CASCADE remove histórico");
            id=0;
            System.out.println("APROVADO: "+checks+" verificações HTTP + PostgreSQL, reinício e cascata.");
        } finally {
            web.parar();
            if(id>0)ocorrencias.excluir(id);
        }
    }
}
