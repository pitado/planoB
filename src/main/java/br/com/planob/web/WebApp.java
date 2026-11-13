package br.com.planob.web;

import br.com.planob.domain.*;
import br.com.planob.repository.*;
import br.com.planob.service.OcorrenciaService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HTTP local: injeta contratos de repositório, sem depender de PostgreSQL. */
public final class WebApp {
    private final OcorrenciaService service;
    private final OcorrenciaRepository ocorrencias;
    private final AcaoRepository acoes;
    private final WebPages pages;
    private HttpServer server;
    private ExecutorService executor;

    public WebApp(OcorrenciaRepository ocorrencias, AcaoRepository acoes) {
        this.ocorrencias=Objects.requireNonNull(ocorrencias);
        this.acoes=Objects.requireNonNull(acoes);
        this.service=new OcorrenciaService(ocorrencias);
        this.pages=new WebPages(ocorrencias,acoes);
    }

    /** Porta 0 é aceita para testes com porta dinâmica. */
    public synchronized int iniciar(int porta) throws IOException {
        if (server!=null) throw new IllegalStateException("Servidor já iniciado.");
        if (porta<0 || porta>65535) throw new IllegalArgumentException("Porta inválida.");
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",porta),0);
        server.createContext("/",this::handle);
        executor=Executors.newFixedThreadPool(5);
        server.setExecutor(executor);
        server.start();
        return server.getAddress().getPort();
    }

    public synchronized void parar(){
        if(server!=null)server.stop(0);
        if(executor!=null)executor.shutdownNow();
        server=null;executor=null;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try{
            String method=exchange.getRequestMethod(),path=exchange.getRequestURI().getPath();
            if(!method.equals("GET")&&!method.equals("POST")){
                exchange.getResponseHeaders().set("Allow","GET, POST");
                render(exchange,405,pages.erro(405,"Método não permitido."));return;
            }
            if(method.equals("GET")){
                switch(path){
                    case "/" -> {render(exchange,200,pages.painel());return;}
                    case "/ocorrencias" -> {
                        Map<String,String> query=parameters(exchange.getRequestURI().getRawQuery());
                        render(exchange,200,pages.listagem(optional(query.get("status"),StatusOcorrencia.class),
                                optional(query.get("tipo"),TipoOcorrencia.class)));return;
                    }
                    case "/ocorrencias/nova" -> {render(exchange,200,pages.formulario(null));return;}
                    case "/sobre" -> {render(exchange,200,pages.sobre());return;}
                    case "/health" -> {send(exchange,200,"OK","text/plain");return;}
                    case "/assets/site.css" -> {css(exchange);return;}
                    default -> {}
                }
            }
            if(method.equals("POST")) {
                checkOrigin(exchange);
                if(path.equals("/ocorrencias")){
                    Map<String,String> form=post(exchange);
                    long id=service.cadastrar(tipo(form),required(form,"bairro"),required(form,"logradouro"),
                            required(form,"descricao"),severidade(form));
                    redirect(exchange,"/ocorrencias/"+id);return;
                }
            }
            String[] p=path.split("/");
            if(p.length>=3 && p[1].equals("ocorrencias") && p[2].matches("[0-9]{1,18}")){
                long id=Long.parseLong(p[2]);
                if(method.equals("GET") && p.length==3){render(exchange,200,pages.detalhe(id));return;}
                if(method.equals("GET") && p.length==4 && p[3].equals("editar")){
                    render(exchange,200,pages.formulario(service.consultar(id)));return;
                }
                if(method.equals("GET") && p.length==4 && p[3].equals("excluir")){
                    render(exchange,200,pages.confirmar(id));return;
                }
                if(method.equals("POST") && p.length==4){
                    Map<String,String> form=post(exchange);
                    switch(p[3]){
                        case "atualizar" -> service.atualizar(id,tipo(form),required(form,"bairro"),
                                required(form,"logradouro"),required(form,"descricao"),severidade(form),
                                StatusOcorrencia.valueOf(required(form,"status")));
                        case "excluir" -> {
                            if(!"sim".equals(form.get("confirmar")))
                                throw new IllegalArgumentException("Confirme a exclusão no formulário.");
                            service.excluir(id);
                            redirect(exchange,"/ocorrencias");return;
                        }
                        case "acoes" -> {
                            service.consultar(id);
                            acoes.criar(AcaoRecuperacao.nova(id,required(form,"titulo"),required(form,"responsavel")));
                        }
                        default -> {render(exchange,404,pages.erro(404,"Rota não encontrada."));return;}
                    }
                    redirect(exchange,"/ocorrencias/"+id);return;
                }
                if(method.equals("POST") && p.length==6 && p[3].equals("acoes")
                        && p[4].matches("[0-9]{1,18}") && p[5].equals("concluir")){
                    post(exchange);
                    service.consultar(id);
                    long acaoId=Long.parseLong(p[4]);
                    if(!acoes.concluir(acaoId,id))throw new NoSuchElementException("Ação não encontrada ou já concluída.");
                    redirect(exchange,"/ocorrencias/"+id);return;
                }
            }
            render(exchange,404,pages.erro(404,"Página não encontrada."));
        }catch(NoSuchElementException e){
            render(exchange,404,pages.erro(404,e.getMessage()));
        }catch(IllegalArgumentException e){
            render(exchange,400,pages.erro(400,e.getMessage()==null?"Dados inválidos.":e.getMessage()));
        }catch(Exception e){
            System.err.println("Falha HTTP: "+e.getClass().getSimpleName());
            render(exchange,500,pages.erro(500,"Não foi possível processar a solicitação."));
        }finally{exchange.close();}
    }

    private static void checkOrigin(HttpExchange x){
        String origin=x.getRequestHeaders().getFirst("Origin");
        if(origin==null)return;
        String host=x.getRequestHeaders().getFirst("Host");
        if(host==null||!origin.equals("http://"+host))
            throw new IllegalArgumentException("Formulário de origem não permitida.");
    }

    private static Map<String,String> post(HttpExchange x) throws IOException{
        String contentType=x.getRequestHeaders().getFirst("Content-Type");
        if(contentType==null||!contentType.toLowerCase().startsWith("application/x-www-form-urlencoded"))
            throw new IllegalArgumentException("Envie um formulário válido.");
        byte[] data=x.getRequestBody().readNBytes(8193);
        if(data.length>8192)throw new IllegalArgumentException("Formulário muito grande.");
        return parameters(new String(data,StandardCharsets.UTF_8));
    }

    private static Map<String,String> parameters(String text){
        Map<String,String> values=new HashMap<>();
        if(text==null||text.isBlank())return values;
        for(String pair:text.split("&")){
            if(pair.isBlank())continue;
            String[] p=pair.split("=",2);
            String name=URLDecoder.decode(p[0],StandardCharsets.UTF_8);
            String value=URLDecoder.decode(p.length==2?p[1]:"",StandardCharsets.UTF_8);
            if(name.isBlank()||values.putIfAbsent(name,value)!=null)
                throw new IllegalArgumentException("Campo duplicado ou inválido: "+name);
        }
        return values;
    }

    private static <E extends Enum<E>> E optional(String raw,Class<E> type){
        return raw==null||raw.isBlank()?null:Enum.valueOf(type,raw);
    }
    private static String required(Map<String,String> form,String key){
        String value=form.get(key);
        if(value==null||value.isBlank())throw new IllegalArgumentException("Campo obrigatório: "+key);
        return value;
    }
    private static TipoOcorrencia tipo(Map<String,String> form){
        return TipoOcorrencia.valueOf(required(form,"tipo"));
    }
    private static int severidade(Map<String,String> form){
        return Integer.parseInt(required(form,"severidade"));
    }
    private static void css(HttpExchange x) throws IOException{
        try(InputStream input=WebApp.class.getResourceAsStream("/static/site.css")){
            if(input==null){send(x,404,"Arquivo não encontrado.","text/plain");return;}
            sendBytes(x,200,input.readAllBytes(),"text/css");
        }
    }
    private static void render(HttpExchange x,int status,String html) throws IOException{
        send(x,status,html,"text/html");
    }
    private static void send(HttpExchange x,int status,String text,String type) throws IOException{
        sendBytes(x,status,text.getBytes(StandardCharsets.UTF_8),type);
    }
    private static void sendBytes(HttpExchange x,int status,byte[] bytes,String type) throws IOException{
        x.getResponseHeaders().set("Content-Type",type+"; charset=UTF-8");
        x.getResponseHeaders().set("Cache-Control","no-store");
        x.getResponseHeaders().set("X-Content-Type-Options","nosniff");
        x.getResponseHeaders().set("X-Frame-Options","DENY");
        x.getResponseHeaders().set("Content-Security-Policy",
            "default-src 'self'; script-src 'none'; style-src 'self'; img-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
        x.sendResponseHeaders(status,bytes.length);
        try(var out=x.getResponseBody()){out.write(bytes);}
    }
    private static void redirect(HttpExchange x,String path) throws IOException{
        x.getResponseHeaders().set("Location",path);
        x.getResponseHeaders().set("Cache-Control","no-store");
        x.sendResponseHeaders(303,-1);
    }
}
