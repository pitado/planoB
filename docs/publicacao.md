# Publicação e histórico do Plano B

Repositório público: https://github.com/pitado/planoB

O código foi publicado em etapas de estrutura, domínio orientado a objetos, banco de dados, interface, testes e documentação. A importação técnica reconstruiu os commits nesta data, de forma transparente; não representa trabalho realizado em datas anteriores.

O histórico Git local original também está preservado no arquivo Plano-B-Historico.gitbundle disponibilizado na conversa. No GitHub, os commits da importação têm autoria de execução automatizada, sem atribuição fictícia de trabalhos a integrantes.

## Conferência antes da AEP

1. Conferir a branch main, o README e as pastas src, database e docs.
2. Abrir a aba Actions e verificar os testes de domínio/HTTP e PostgreSQL real.
3. Consultar o documento de escopo, os diagramas UML/DER e o PDF formal.
4. Registrar nomes e contribuições reais dos integrantes no cronograma.
5. Confirmar com o professor como avaliará a aderência ao planejamento do primeiro bimestre, que não foi entregue.

## Reorganização por sprints (13/11/2026)

O código completo anterior continua disponível na branch `arquivo/mvp-completo-2026-11-13`. A branch `main` foi reduzida temporariamente a uma base de domínio compilável e depois recebeu as sprints de Miguel (PR #4), Gustavo (PR #6) e Lucas (PR #5). A integração final adiciona a inicialização HTTP com JDBC e testes de banco via navegador. Os commits antigos não são atribuídos retroativamente a novos integrantes; cada participação efetiva deve ser verificada pela equipe.

## Execução final integrada

`docker compose up -d postgres` e `mvn -q compile exec:java` iniciam banco e site. Os testes `scripts/test-domain.sh`, `scripts/test-web.sh`, `scripts/test-db.sh` e `scripts/test-http-db.sh` validam POO, HTTP, JDBC e o fluxo persistente. A aplicação continua local, sem autenticação nem validação de relatos.

O PDF do trabalho, atualizado após a integração, deve trazer link público correto, requisitos RF01–RF08, cronograma por integrante e diagramas; a falta da primeira entrega deve ser informada com transparência.
