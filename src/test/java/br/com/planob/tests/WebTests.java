package br.com.planob.tests;

import br.com.planob.web.DemoRepositories;
import br.com.planob.web.WebApp;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Testes HTTP da Sprint Lucas — sem banco real.
 * O PostgreSQL e os testes JDBC pertencem à Sprint Gustavo.
 */
public final class WebTests {
    private static final HttpClient HTTP=HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER).build();
    private static int testes;
    private static String base;
    private WebTests(){}

    private static HttpResponse<String> call(String method,String path,String body,String contentType,String origin) throws Exception {
        HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(base+path));
        if(origin!=null)builder.header("Origin",origin);
        if(method.equals("POST")){
            builder.header("Content-Type",contentType==null?"application/x-www-form-urlencoded; charset=UTF-8":contentType);
            builder.POST(HttpRequest.BodyPublishers.ofString(body==null?"":body,StandardCharsets.UTF_8));
        }else if(method.equals("GET"))builder.GET();
        else builder.method(method,HttpRequest.BodyPublishers.noBody());
        return HTTP.send(builder.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private static HttpResponse<String> get(String path) throws Exception {
        return call("GET",path,null,null,null);
    }
    private static HttpResponse<String> post(String path,String body) throws Exception {
        return call("POST",path,body,null,null);
    }
    private static void check(boolean success,String message){
        if(!success)throw new AssertionError(message);
        testes++;
    }
    private static void status(HttpResponse<String> r,int code){
        check(r.statusCode()==code,"HTTP "+code+" esperado, retornou "+r.statusCode()+": "+r.body());
    }
    private static void has(HttpResponse<String> r,String needle){
        check(r.body().contains(needle),"Resposta não contém: "+needle);
    }
    private static String encode(String v){return URLEncoder.encode(v,StandardCharsets.UTF_8);}

    public static void main(String[] args) throws Exception {
        var repo=new DemoRepositories.Ocorrencias();
        var actions=new DemoRepositories.Acoes();
        WebApp app=new WebApp(repo,actions);
        int port=app.iniciar(0);
        base="http://127.0.0.1:"+port;
        try{
            var home=get("/");
            status(home,200);has(home,"Organizar o agora");
            check(home.headers().firstValue("Content-Security-Policy").orElse("").contains("script-src 'none'"),"CSP presente");
            var css=get("/assets/site.css");
            status(css,200);has(css,"--green");
            status(get("/health"),200);
            status(get("/sobre"),200);
            var blank=get("/ocorrencias");status(blank,200);has(blank,"Nenhuma ocorrência");
            status(get("/ocorrencias/nova"),200);

            String body="tipo=VIA_OBSTRUIDA&bairro=Centro&logradouro=Rua+das+Flores&descricao="
                    +encode("<script>alert(1)</script> Relato fictício")+"&severidade=3";
            var created=post("/ocorrencias",body);
            status(created,303);
            check("/ocorrencias/1".equals(created.headers().firstValue("Location").orElse("")),"Redirect do cadastro");
            check(repo.listar(null,null).size()==1,"Cadastro gravado no repositório de teste");
            var detail=get("/ocorrencias/1");
            status(detail,200);has(detail,"&lt;script&gt;");
            check(!detail.body().contains("<script>alert(1)</script>"),"Sem XSS no HTML");
            has(detail,"Ações de recuperação");has(detail,"Aberta");

            var filtered=get("/ocorrencias?status=ABERTA&tipo=VIA_OBSTRUIDA");
            status(filtered,200);has(filtered,"1 registro(s)");
            var other=get("/ocorrencias?tipo=ALAGAMENTO");
            status(other,200);has(other,"0 registro(s)");
            status(get("/ocorrencias?tipo=INVALIDO"),400);

            status(get("/ocorrencias/1/editar"),200);
            var invalidJump=post("/ocorrencias/1/atualizar",
                "tipo=VIA_OBSTRUIDA&bairro=Centro&logradouro=Rua+das+Flores&descricao=Nova&severidade=3&status=RESOLVIDA");
            status(invalidJump,400);has(invalidJump,"Transição");
            var updated=post("/ocorrencias/1/atualizar",
                "tipo=ALAGAMENTO&bairro=Centro&logradouro=Rua+das+Flores&descricao=Atualizada&severidade=4&status=EM_ANALISE");
            status(updated,303);
            var after=get("/ocorrencias/1");
            has(after,"Atualizada");has(after,"Em análise");has(after,"Histórico");
            check(repo.historico(1).size()==2,"Histórico de transições com abertura e análise");

            status(post("/ocorrencias/1/acoes","titulo=Verificar+rua&responsavel=Equipe+do+bairro"),303);
            var withAction=get("/ocorrencias/1");has(withAction,"Verificar rua");has(withAction,"Equipe do bairro");
            status(post("/ocorrencias/1/acoes/1/concluir",""),303);
            has(get("/ocorrencias/1"),"Concluída");
            status(post("/ocorrencias/1/acoes/1/concluir",""),404);
            status(post("/ocorrencias/1/acoes/999/concluir",""),404);

            status(post("/ocorrencias","tipo=ALAGAMENTO&bairro=A&logradouro=Rua&descricao=Invalida&severidade=9"),400);
            status(post("/ocorrencias","tipo=ALAGAMENTO&tipo=VIA_OBSTRUIDA&bairro=A&logradouro=Rua&descricao=Teste&severidade=3"),400);
            status(call("POST","/ocorrencias",body,"text/plain",null),400);
            status(call("POST","/ocorrencias",body,null,"https://site-fora.example"),400);
            status(call("PUT","/ocorrencias",null,null,null),405);

            var confirmation=get("/ocorrencias/1/excluir");
            status(confirmation,200);has(confirmation,"Sim, excluir definitivamente");
            status(post("/ocorrencias/1/excluir",""),400);
            var deleted=post("/ocorrencias/1/excluir","confirmar=sim");
            status(deleted,303);
            status(get("/ocorrencias/1"),404);
            status(get("/pagina-que-nao-existe"),404);
            has(get("/ocorrencias"),"0 registro(s)");
            System.out.println("APROVADO: "+testes+" verificações HTTP, CRUD, filtros, histórico, ações e segurança.");
        }finally{app.parar();}
    }
}
