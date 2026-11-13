# Sprints do Plano B — trabalho real em equipe

## Contexto

O projeto foi reorganizado em 13/11/2026 para distribuir responsabilidades de forma mais equilibrada. O protótipo anterior continua preservado, sem alteração de histórico, em [`arquivo/mvp-completo-2026-11-13`](https://github.com/pitado/planoB/tree/arquivo/mvp-completo-2026-11-13). **O código anterior não se transforma em autoria de Lucas ou Gustavo por estar arquivado.** Cada integrante deverá estudar, escrever ou modificar código de maneira substantiva, testar e abrir sua própria Pull Request.

## Sprint 1 — Miguel: núcleo e regras de negócio

**Issue:** [#1](https://github.com/pitado/planoB/issues/1)
**Branch de trabalho:** `sprint/miguel-dominio`

Área: `src/main/java/br/com/planob/domain/`, `service/`, `src/test/java/` e UML.

Entregar política real de transições de status e validações, explicação do índice orientativo, testes positivos e negativos, ajuste de UML e documentação de decisões.

**Critério de aceite:** domínio compila em Java 21, testes passam e a PR demonstra mudanças funcionais próprias.

## Sprint 2 — Lucas: interface web

**Issue:** [#2](https://github.com/pitado/planoB/issues/2)
**Branch de trabalho:** `sprint/lucas-interface`

Área: `src/main/java/br/com/planob/web/`, `src/main/resources/static/`, testes HTTP e interface.

Entregar páginas do painel, listagem, cadastro, edição, detalhe, exclusão confirmada, ações de recuperação e feedback de erros. Consumir as interfaces de repositórios e o serviço, sem SQL direto na camada web.

**Critério de aceite:** demonstrar CRUD via HTTP com repositórios de teste, layout responsivo e testes próprios na PR.

## Sprint 3 — Gustavo: PostgreSQL e persistência

**Issue:** [#3](https://github.com/pitado/planoB/issues/3)
**Branch de trabalho:** `sprint/gustavo-postgresql`

Área: `database/`, `src/main/java/br/com/planob/repository/`, `config/` e testes JDBC.

Entregar script SQL de criação e seed fictícia, repositórios JDBC parametrizados, persistência de ocorrência e ação, histórico de status em transação e testes em PostgreSQL 16 real.

**Critério de aceite:** CRUD e integridade relacional comprovados no banco real, com CI verde e DER coerente com o esquema.

## Acordos de integração

1. Cada participante cria commits com sua própria conta e descreve o que efetivamente alterou.
2. Uma PR por sprint, vinculada à sua issue; revisão antes de mesclar.
3. A branch `main` é uma **base compilável**, não a versão completa do site; integraremos as Sprints 1, 2 e 3 quando os critérios forem cumpridos.
4. Atualizar o `README.md`, a `docs/validacao.md`, o cronograma e a documentação final após a integração.
5. Não remover nem reescrever o histórico anterior para sugerir datas ou autorias falsas.

## Matriz de responsabilidades

| Integrante | Escopo predominante | Prova de contribuição |
| --- | --- | --- |
| Miguel | Domínio, regras e integração | Commits e PR da issue #1 |
| Lucas | Interface, rotas e testes HTTP | Commits e PR da issue #2 |
| Gustavo | Banco, JDBC e testes reais | Commits e PR da issue #3 |

Nomes completos e RAs serão acrescentados **depois** de fornecidos pelos próprios integrantes; não estão inventados neste planejamento.
