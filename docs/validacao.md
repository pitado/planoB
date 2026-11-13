# Matriz de conformidade e validação da AEP

## Requisitos de correção

| Critério do PDF | Peso | Evidência neste projeto | Situação de verificação |
|---|---:|---|---|
| Aderência ao planejamento anterior | 0,2 | Escopo retrospectivo no README e `docs/escopo-e-arquitetura.md` | **Não comprovável**: 1º bimestre não foi entregue |
| POO: interface/classe abstrata, herança e `@Override` | 0,3 | `Ocorrencia`, subclasses, `OcorrenciaRepository`, `AcaoRepository` | Compilado e testado localmente |
| CRUD funcional da entidade principal | 0,3 | `JdbcOcorrenciaRepository`, `OcorrenciaService`, formulários GET/POST | Fluxo HTTP testado com dublês; JDBC depende do banco |
| Persistência em BD, SQL em `/database` | 0,2 | `database/schema.sql`, `JdbcOcorrenciaRepository`, `JdbcAcaoRepository` | Código compilado; integração real automatizada na CI, ainda não executada localmente |

## Verificações realizadas em 13/11/2026

- `bash scripts/test-domain.sh`: **35 verificações de domínio e casos de uso aprovadas**.
- `bash scripts/test-domain.sh`: **22 verificações HTTP aprovadas**, incluindo os caminhos completos de criação, consulta, alteração e exclusão.
- Compilação local: `javac --release 21` **sem erros**.
- Integração PostgreSQL: **pendente** no ambiente local, que não tem servidor PostgreSQL, Maven nem dependências pré-instaladas; preparada para rodar pelo `scripts/test-db.sh` e GitHub Actions após publicação.

Resultados de testes não executados nunca devem ser descritos como aprovados. O workflow configura PostgreSQL real para validar operações de INSERT, SELECT, UPDATE, DELETE, histórico transacional e cascata de ações.

## Plano de demonstração ao professor

1. Subir `docker compose up -d postgres` e confirmar que o serviço está saudável.
2. Executar `bash scripts/test-db.sh` e registrar a saída de sucesso.
3. Iniciar `mvn -q compile exec:java` e abrir `http://localhost:8080`.
4. Demonstrar as quatro operações de CRUD em uma ocorrência fictícia.
5. Atualizar a situação e mostrar o registro automático do histórico.
6. Criar e concluir uma ação de recuperação vinculada à ocorrência.
7. Explicar o `@Override` nas quatro subclasses, o contrato da interface e a composição 1:N.
8. Exibir `schema.sql` e `docs/diagrams`.
9. Conferir os commits reais de cada integrante, quando houver.

## Limitações e riscos

- Não existe histórico de primeira entrega, logo os 0,2 de aderência podem não ser atribuídos.
- A rubrica tem inconsistência entre 0,3 (tabela de POO) e 0,4 (observação final); confirmar interpretação com professor.
- Interface sem autenticação, exclusivamente local. Não publicar como sistema público.
- Não há validação de ocorrências reais nem conexão com órgãos públicos.
- Cálculo de prioridade é didático; não usar para tomada de decisão operacional em emergência.
