# Plano B

**Quando a cidade enfrenta uma crise, ter um próximo passo faz a diferença.**

O **Plano B** é um protótipo de resiliência urbana e organização comunitária para moradores, pequenos estabelecimentos e grupos locais acompanharem impactos de temporais, alagamentos, danos estruturais, bloqueios de vias e interrupções de energia. O tema nasceu da necessidade de organizar informações e tarefas de recuperação após acontecimentos críticos em **Maringá (PR)**.

**É um protótipo acadêmico local com dados fictícios**, não um serviço de emergência nem fonte de alertas oficiais. Não publicar sem autenticação, proteção de dados e moderação.

## Requisitos funcionais — AEP

| Código | O sistema deve... | Evidência |
|---|---|---|
| RF01 | Cadastrar ocorrência com localização e severidade | Formulário e JDBC |
| RF02 | Consultar/listar com filtros por tipo e situação | Tela de listagem |
| RF03 | Atualizar dados e situação da ocorrência | Edição e política de transições |
| RF04 | Excluir ocorrência e dependentes | Confirmação e ON DELETE CASCADE |
| RF05 | Calcular índice ilustrativo por tipo e severidade | Herança e @Override |
| RF06 | Adicionar e concluir ações de recuperação | Formulário de ações e JDBC |
| RF07 | Registrar histórico de alterações de situação | Transações PostgreSQL |
| RF08 | Mostrar painel com totais por situação | Página inicial |

**O índice é apenas orientativo, não uma avaliação técnica de perigo ou urgência.**

## Rodar a aplicação integrada

**Pré-requisitos:** Java 21, Maven 3.9+ e Docker Compose. Portas locais 5432 (banco) e 8080 (site).

1. Inicie o PostgreSQL 16 (no primeiro uso, aplica schema e seed fictícia):

   ```bash
   docker compose up -d postgres
   ```

2. Execute a aplicação integrada ao banco:

   ```bash
   mvn -q compile exec:java
   ```

3. Abra **http://127.0.0.1:8080**. Cadastre, edite, consulte, filtre e exclua ocorrências, além de registrar tarefas.

O ponto de entrada `br.com.planob.Main` usa `WebApp` com `JdbcOcorrenciaRepository` e `JdbcAcaoRepository`. **O modo integrado persiste os dados no PostgreSQL**. Se o banco estiver indisponível, a inicialização falha com uma mensagem de diagnóstico.

| Variável | Padrão de desenvolvimento local |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/plano_b` |
| `DB_USER` | `planob` |
| `DB_PASSWORD` | `planob_dev` |
| `PORT` | `8080` |

Os valores são apenas demonstrativos; não os utilize em produção.

### Testes

```bash
bash scripts/test-domain.sh    # POO e regras
bash scripts/test-web.sh       # HTTP e CRUD com dublês em memória
bash scripts/test-db.sh        # JDBC em PostgreSQL 16 real
bash scripts/test-http-db.sh   # HTTP + banco, reinício e exclusão em cascata
```

Os últimos dois scripts precisam do PostgreSQL funcionando. O [GitHub Actions](https://github.com/pitado/planoB/actions) inicia PostgreSQL 16 isolado e testa as camadas.

**Modo demonstrativo sem banco:** execute `bash scripts/test-web.sh` e `java -cp ".build/classes:src/main/resources" br.com.planob.web.WebMain`. Esse modo usa memória volátil, não comprova persistência e **não atende isoladamente ao critério de BD da AEP**.

## Justificativa e arquitetura

| Camada | Tecnologia | Por quê |
|---|---|---|
| Domínio | Java 21, classe abstrata, interfaces | Tipagem, herança, polimorfismo |
| Regras | `OcorrenciaService` e `PoliticaTransicaoStatus` | Regras independentes de HTTP |
| Interface | HTTP Server JDK, HTML e CSS responsivo | Simplicidade local |
| Persistência | JDBC parametrizado | CRUD real e transações |
| Banco | PostgreSQL 16 | Integridade e relacionamentos 1:N |
| Ambiente | Docker Compose | Execução reproduzível |

**Fluxo:** navegador → `WebApp` → `OcorrenciaService` → contratos de repositório → JDBC → PostgreSQL.

**POO demonstrada:** classe abstrata `Ocorrencia`; subclasses `FalhaEnergia`, `ViaObstruida`, `Alagamento` e `DanoEstrutural`; `@Override` em `pesoOperacional()`; interfaces de repositório; uma ocorrência com várias ações.

[Diagrama UML](docs/diagrams/classes.puml) · [DER](docs/diagrams/der.mmd) · [SQL](database/schema.sql) · [Escopo](docs/escopo-e-arquitetura.md)

## Cronograma e responsabilidades

*Registra a reorganização da equipe no segundo bimestre; não constitui prova de uma primeira entrega.*

| Data de referência | Etapa | Responsável | Evidência |
|---|---|---|---|
| 13/11/2026 | Domínio, POO e UML | Miguel | [PR #4](https://github.com/pitado/planoB/pull/4) |
| 13/11/2026 | Interface, rotas e testes HTTP | Lucas Guimarães Pereira | [PR #5](https://github.com/pitado/planoB/pull/5) |
| 13/11/2026 | PostgreSQL, JDBC e histórico | Gustavo Krensiglova Lubacheski | [PR #6](https://github.com/pitado/planoB/pull/6) |
| 13/11/2026 | Integração e testes completos | Equipe / assistência IA | [Actions](https://github.com/pitado/planoB/actions) |

**Transparência:** implementações e revisões receberam assistência de IA. Commits feitos por uma conta conectada não provam que seu titular escreveu manualmente o código; a equipe precisa estudar e saber explicar suas contribuições efetivas.

## Estrutura

```text
src/main/java/br/com/planob/  Domínio, serviço, JDBC e HTTP
src/main/resources/static/    CSS responsivo
src/test/java/br/com/planob/  Testes de domínio, web e banco
database/                     schema.sql, seed.sql e instruções
docs/                         Diagramas, escopo e cronograma
scripts/                      Scripts de teste
.github/workflows/            CI
```

O projeto se relaciona aos **ODS 11 (Cidades e Comunidades Sustentáveis)** e **13 (Ação Climática)**. Veja também [cronograma](docs/cronograma.md), [validação](docs/validacao.md) e [arquitetura](docs/escopo-e-arquitetura.md).

**Pendência acadêmica:** a primeira entrega não foi realizada. É necessário consultar o professor sobre o critério de aderência ao planejamento.
