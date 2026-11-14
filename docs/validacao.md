# Validação da AEP — Plano B

A implementação é um **protótipo acadêmico local** de resiliência urbana alinhado aos ODS 11 e 13. Repositório: https://github.com/pitado/planoB

## Evidências técnicas

| Critério do segundo bimestre | Evidência no código | Comprovação |
|---|---|---|
| Aderência ao escopo | Requisitos RF01–RF08 do README; entidades Ocorrência/Ação/Histórico | Confrontar com o README e o PDF acadêmico atualizado (distribuído separadamente); 1º bimestre não entregue |
| POO | Classe abstrata `Ocorrencia`, quatro subclasses, `pesoOperacional()` com `@Override`, interfaces de repositório | `bash scripts/test-domain.sh` |
| CRUD funcional | `WebApp` + `OcorrenciaService`, formulários HTML, filtros, edição, exclusão confirmada | `bash scripts/test-web.sh` |
| Persistência | `database/schema.sql`, `seed.sql`, JDBC, transações, FK e histórico | `bash scripts/test-db.sh` no PostgreSQL 16 |
| Integração ponta a ponta | `br.com.planob.Main` injeta JDBC em `WebApp`; fluxo HTTP e reinício | `bash scripts/test-http-db.sh`, com PostgreSQL real |
| Colaboração | [PR #4](https://github.com/pitado/planoB/pull/4), [PR #5](https://github.com/pitado/planoB/pull/5), [PR #6](https://github.com/pitado/planoB/pull/6) | Revisar atividades efetivas e assistência por IA |
| CI | [Run da main #43533543082](https://github.com/pitado/planoB/actions/runs/43533543082) | Jobs domínio, interface-http e postgresql aprovados, inclusive HTTP + JDBC |

A [execução #43533543082](https://github.com/pitado/planoB/actions/runs/43533543082) na `main` concluiu com sucesso. Os testes de memória existem para isolar a camada HTTP. **Não servem como evidência de persistência**. Os testes JDBC e HTTP+JDBC usam banco PostgreSQL real e verificam a sobrevivência do registro a um reinício do servidor, além da exclusão em cascata.

## Execução para a banca

1. `docker compose up -d postgres`
2. `mvn -q compile exec:java`
3. Abrir `http://127.0.0.1:8080` (somente local).
4. Cadastrar ocorrência fictícia e consultar o registro.
5. Alterar a situação para "Em análise" e verificar histórico.
6. Criar ação e concluí-la; reiniciar o servidor e observar persistência.
7. Excluir com confirmação; conferir remoção das ações e do histórico.
8. Mostrar `Ocorrencia` abstrata, subclasses com `@Override` e diagramas UML/DER.

## Itens acadêmicos que dependem da equipe

- [ ] Preencher/confirmar identificação individual no documento de submissão (RAs devem permanecer fora do GitHub público).
- [ ] Revisar código e explicar as contribuições efetivas de Miguel, Lucas e Gustavo; assistência de IA não equivale a participação manual.
- [x] Gerar PDF final com requisitos, UML, DER e cronograma. **Pendente:** conferência e anexação pela equipe na entrega institucional.
- [ ] Esclarecer com o professor o critério de aderência: **a 1ª entrega não foi realizada**.
- [ ] Realizar demonstração manual em máquina com Java 21, Maven e Docker.

**Segurança:** sem autenticação, sem validação de relatos e sem autorização para uso público; dados fictícios apenas.
