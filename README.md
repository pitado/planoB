# Plano B

**Plataforma acadêmica de resiliência urbana**, inspirada na necessidade de organizar interrupções e ações de recuperação após temporais em Maringá (PR). O projeto **não** oferece alertas oficiais, previsões meteorológicas nem atendimento de emergência.

> **Situação da AEP:** este repositório inclui planejamento retrospectivo e implementação para o 2º bimestre de 2026. O planejamento do 1º bimestre **não foi entregue na época**; a avaliação da aderência depende do professor. Não se deve representar o material atual como uma entrega anterior.

## Objetivo e ODS

ODS 11 — Cidades e comunidades sustentáveis; ODS 13 — Ação contra a mudança global do clima. O Plano B registra ocorrências por bairro, calcula prioridade operacional orientativa, relaciona ações de recuperação e mantém um histórico das mudanças de status.

## Requisitos funcionais

| ID | Requisito | Evidência |
|---|---|---|
| RF01 | Cadastrar ocorrências com tipo, bairro, endereço, descrição e severidade (1–5). | Formulário e `JdbcOcorrenciaRepository.criar` |
| RF02 | Listar, consultar e filtrar ocorrências por status e tipo. | Telas de listagem e detalhe |
| RF03 | Editar ocorrências existentes. | Formulário de edição e `atualizar` |
| RF04 | Excluir uma ocorrência e seus vínculos. | Exclusão com confirmação |
| RF05 | Classificar a prioridade orientativa segundo tipo e severidade. | `Ocorrencia.calcularIndiceImpacto` e subclasses |
| RF06 | Criar e concluir ações de recuperação associadas a ocorrências. | `JdbcAcaoRepository` e detalhe |
| RF07 | Registrar histórico quando a situação de uma ocorrência mudar. | Tabela `historico_status` |
| RF08 | Exibir resumo de ocorrências por situação. | Painel inicial |

## Cronograma de execução (planejamento elaborado no 2º bimestre)

| Etapa | Data de referência | Responsável |
|---|---|---|
| Levantamento do problema, escopo e requisitos | 13/11/2026 | Equipe do projeto (a definir) |
| Modelo orientado a objetos, diagramas e banco | 13/11/2026 | Equipe do projeto (a definir) |
| CRUD, persistência e ações de recuperação | 13/11/2026 | Equipe do projeto (a definir) |
| Testes, validação e documentação | 13/11/2026 | Equipe do projeto (a definir) |

**Atribuições dos integrantes:** preencher com os nomes e contribuições reais. Não atribuir commits nem tarefas a colegas que não participaram.

## Executar

Requisitos: **Java 21**, **Maven 3.9+**, **Docker com Compose** (ou PostgreSQL 16 acessível). Em terminal:

```bash
docker compose up -d postgres
mvn -q compile exec:java
```

Acesse `http://localhost:8080`. O banco de desenvolvimento usa as credenciais de exemplo do `compose.yaml`. Se utilizar outro servidor, ajuste `DB_URL`, `DB_USER` e `DB_PASSWORD` (não versione credenciais de produção). O Docker executa `schema.sql` e `seed.sql` apenas no **primeiro** início de um volume novo.

### Testes

```bash
bash scripts/test-domain.sh
# Com Docker iniciado e dependências Maven disponíveis:
bash scripts/test-db.sh
```

O primeiro script usa apenas o JDK para testar regras de POO sem internet. O segundo exige PostgreSQL e o driver JDBC resolvido pelo Maven.

## Materiais acadêmicos

- [Documento formal da AEP (PDF)](docs/Plano-B-AEP-Documentacao.pdf) — material elaborado no 2º bimestre, não substitui retroativamente a primeira entrega.
- [Diagrama de classes visual](docs/diagrams/classes.svg) e [DER visual](docs/diagrams/der.svg).
- [Matriz de conformidade e testes](docs/validacao.md).
- [Publicação e histórico de commits](docs/publicacao.md).
- [Descoberta, justificativa e arquitetura](docs/escopo-e-arquitetura.md).
- [Cronograma e responsabilidades](docs/cronograma.md).

**Repositório público:** https://github.com/pitado/planoB. **Antes de entregar ao professor:** verificar os testes PostgreSQL no GitHub Actions, preencher integrantes e contribuições reais e discutir com o professor a ausência da primeira entrega.

## Estrutura

- `/src/main/java`: domínio (POO), serviços, repositórios JDBC e servidor web;
- `/database`: criação de tabelas, restrições e dados **fictícios** para demonstração;
- `/docs`: levantamento, justificativas, cronograma e diagramas;
- `/src/test`: testes de domínio e integração;
- `/.github/workflows`: integração contínua.

## Limites e uso seguro

A prioridade calculada é apenas um **índice acadêmico de triagem**, não uma indicação de risco real. Os registros da demonstração são fictícios. O aplicativo é um MVP para rodar localmente: **não possui login nem mecanismos completos de autorização**, portanto não deve ser publicado na internet sem controles adicionais. Em emergência real, procure canais oficiais e serviços públicos apropriados.

## Licença e autoria

Projeto acadêmico desenvolvido com apoio de IA. Commits gerados neste ambiente representam implementação assistida, não são prova de colaboração de outros integrantes.
