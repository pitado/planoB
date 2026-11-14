# Sprints do Plano B — registro do trabalho em equipe

O projeto foi reorganizado no **segundo bimestre, em 13/11/2026**. O código anterior ficou preservado na branch [arquivo/mvp-completo-2026-11-13](https://github.com/pitado/planoB/tree/arquivo/mvp-completo-2026-11-13), sem falsificação de datas ou autorias.

| Sprint | Responsável | Escopo | Evidência |
|---|---|---|---|
| 1 — Domínio e regras de negócio | Miguel | Validações, classes abstratas, transições de status, índice explicável, UML e testes | [PR #4](https://github.com/pitado/planoB/pull/4) |
| 2 — Interface web | Lucas Guimarães Pereira | Painel, listagem, formulários, filtros, ações, histórico e testes HTTP | [PR #5](https://github.com/pitado/planoB/pull/5) |
| 3 — PostgreSQL | Gustavo Krensiglova Lubacheski | SQL, JDBC parametrizado, integridade, transações, testes reais | [PR #6](https://github.com/pitado/planoB/pull/6) |
| Integração | Equipe / assistência de IA | Inicialização HTTP com JDBC, testes ponta a ponta, documentação final | [Actions](https://github.com/pitado/planoB/actions) |

## Como os módulos se conectam

Navegador → `WebApp` (Lucas) → `OcorrenciaService`/domínio (Miguel) → contratos de repositório → implementações JDBC (Gustavo) → PostgreSQL. Os testes do sistema completo estão em `WebDbTests` e `scripts/test-http-db.sh`.

## Critérios de integração

- [x] Sprint de domínio integrada na `main`.
- [x] Persistência PostgreSQL integrada na `main`.
- [x] Interface HTTP integrada na `main` com CI que inclui POO, web e JDBC.
- [x] Testar no GitHub Actions o novo fluxo HTTP + PostgreSQL (run [#43533543082](https://github.com/pitado/planoB/actions/runs/43533543082)); integração [PR #7](https://github.com/pitado/planoB/pull/7) mesclada.
- [ ] Revisar o código individualmente, registrar contribuição real, validar documento de submissão.

**Atenção à autoria:** parte do código e de commits utilizou assistência de IA e conexões GitHub. A associação de um commit à conta de um participante não significa que esse participante digitou manualmente seu conteúdo. Cada integrante deve revisar, entender e relatar apenas aquilo que de fato fez. Os RAs ficam somente na documentação acadêmica restrita.
