# Interface web — Sprint 2 (Lucas)

Esta camada implementa a **interface local** para organizar registros fictícios de impactos comunitários. Contém rotas HTTP, páginas, formulários responsivos e testes de ponta a ponta com os repositórios de demonstração.

A implementação foi elaborada com assistência de IA a pedido do responsável pelo repositório. **Não atribua automaticamente o código ao Lucas**: para cumprir os combinados de colaboração, ele deve revisar, testar, compreender e registrar as próprias contribuições no GitHub antes de assumir a entrega.

## Como executar (Java 21)

Sem precisar instalar PostgreSQL, execute:

```bash
bash scripts/test-web.sh
java -cp ".build/classes:src/main/resources" br.com.planob.web.WebMain
```

Abra http://127.0.0.1:8080. Para alterar a porta: `PORT=8081 java -cp ".build/classes:src/main/resources" br.com.planob.web.WebMain`.

**Os dados são armazenados apenas em memória** neste modo de demonstração e desaparecem ao encerrar o Java. A persistência JDBC/PostgreSQL da Sprint 3 está implementada e é usada pelo ponto de entrada `br.com.planob.Main`. O servidor recebe os repositórios no construtor de `WebApp`, sem SQL na camada web.

## Executar o sistema completo com PostgreSQL

Na raiz do repositório: `docker compose up -d postgres` e `mvn -q compile exec:java`. O endereço é `http://127.0.0.1:8080`. Execute `bash scripts/test-http-db.sh` para validar CRUD e histórico persistentes pelo HTTP. O comando `WebMain` descrito acima serve apenas à demonstração isolada, sem banco.

## Rotas implementadas

| Método | Caminho | Função |
|---|---|---|
| GET | `/` | Visão geral, totalizadores, registros recentes |
| GET | `/ocorrencias` | Listagem, filtros `tipo` e `status` |
| GET | `/ocorrencias/nova` | Cadastro |
| POST | `/ocorrencias` | Criar ocorrência |
| GET | `/ocorrencias/{id}` | Detalhe, prioridade, histórico e ações |
| GET | `/ocorrencias/{id}/editar` | Edição |
| POST | `/ocorrencias/{id}/atualizar` | Alterar dados e status validado |
| GET | `/ocorrencias/{id}/excluir` | Confirmação de exclusão |
| POST | `/ocorrencias/{id}/excluir` | Exclusão (exige `confirmar=sim`) |
| POST | `/ocorrencias/{id}/acoes` | Adicionar ação de recuperação |
| POST | `/ocorrencias/{id}/acoes/{acaoId}/concluir` | Concluir ação |
| GET | `/sobre`, `/health`, `/assets/site.css` | Informações, saúde e estilo |

## Testes e garantias

`bash scripts/test-web.sh` executa os testes existentes de domínio e dezenas de verificações HTTP: cadastro, edição, regras de status, filtros, histórico, ações, conclusão, exclusão confirmada, erros 400/404/405, tamanho e formato de formulário, escape de HTML/XSS, cabeçalhos de segurança e proteção básica por origem.

A interface não contém SQL; `OcorrenciaService`, `OcorrenciaRepository` e `AcaoRepository` são dependências injetadas. A UI respeita `PoliticaTransicaoStatus` definida pela Sprint 1: o formulário de edição oferece apenas transições permitidas; o servidor também impede saltos de situação via HTTP.

## Limites e segurança

Servidor ligado somente em `127.0.0.1`: sem autenticação, sem autorização por usuário, sem moderação e sem persistência no modo de demonstração. **Não publicar na internet nem registrar dados reais de terceiros**. O índice calculado é orientativo, não uma avaliação oficial de urgência.

A Sprint 3 acrescentou persistência real; os testes HTTP + JDBC contra PostgreSQL estão em `WebDbTests` e devem passar no GitHub Actions antes de considerar a integração validada.
