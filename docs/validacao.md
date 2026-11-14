# Validação do Plano B após a divisão em sprints

**Situação em 13/11/2026:** a branch `main` é um ponto de partida de desenvolvimento com domínio Java e contratos de persistência. O MVP completo foi arquivado em [`arquivo/mvp-completo-2026-11-13`](https://github.com/pitado/planoB/tree/arquivo/mvp-completo-2026-11-13), sem alteração retroativa dos commits.

| Critério | Evidência na main | Responsável / situação |
|---|---|---|
| Planejamento anterior | Não entregue no primeiro bimestre | Depende de orientação do professor |
| Classe abstrata, herança e polimorfismo | `Ocorrencia`, subclasses e `@Override`; política de transições e justificativa do índice | [Sprint 1 integrada — PR #4](https://github.com/pitado/planoB/pull/4) |
| CRUD funcional pela interface | Contrato `OcorrenciaRepository` e serviço | Pendente Sprint 2 + integração |
| CRUD persistente no PostgreSQL | `JdbcOcorrenciaRepository`, `JdbcAcaoRepository`, `JdbcIntegrationTests` | Sprint 3 em PR (`sprint/gustavo-postgresql`); aguardando CI |
| Scripts SQL em `/database` | `schema.sql`, `seed.sql` e README | Sprint 3 em PR |
| UML e DER | Arquivos de modelagem em `docs/diagrams` | Revisar após PRs |
| Testes e prova de funcionamento | `bash scripts/test-domain.sh`; `bash scripts/test-db.sh` (PostgreSQL real) | Verificar Actions na main; reativar integração após PRs |

**Evidência de CI do domínio:** [GitHub Actions na main, aprovado](https://github.com/pitado/planoB/actions/runs/43488876141). A interface HTTP e o PostgreSQL continuam sendo atividades abertas de Lucas e Gustavo, respectivamente.

**O que não é permitido afirmar:** que a versão atual da `main` já tem CRUD completo em PostgreSQL, interface web funcional ou que os integrantes participaram de commits antigos. Os testes históricos aprovados pertencem ao MVP arquivado.

## Critérios finais para a entrega

- [ ] Sprint 1 revisada e integrada.
- [ ] Sprint 2 revisada e integrada.
- [ ] Sprint 3 revisada e integrada.
- [ ] Fluxo ponta a ponta via HTTP e PostgreSQL real aprovado.
- [ ] Arquivos SQL, diagramas, cronograma e PDF compatíveis com a versão final.
- [ ] Nomes, RAs e contribuições verdadeiras registrados.
- [ ] Situação da primeira entrega esclarecida com o professor.
