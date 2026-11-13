# Plano B

**Quando a cidade enfrenta uma crise, ter um próximo passo faz a diferença.**

O **Plano B** é uma plataforma de **resiliência urbana e organização comunitária** pensada para ajudar moradores, comunidades e pequenos estabelecimentos a lidar com momentos críticos, como temporais, alagamentos, quedas de energia, danos estruturais e vias bloqueadas.

A proposta nasceu da observação dos impactos de eventos climáticos severos em **Maringá, Paraná**. Em situações assim, os problemas não acontecem isoladamente: uma árvore caída pode bloquear uma rua, uma interrupção de energia pode prejudicar estabelecimentos e uma família pode precisar organizar várias ações antes de retomar sua rotina.

O Plano B busca reunir essas informações de maneira simples e organizada, permitindo **registrar ocorrências, acompanhar sua situação e planejar ações de recuperação**.

> **Estado atual:** o núcleo de regras de negócio está disponível em Java. A interface web e a persistência PostgreSQL estão sendo desenvolvidas em sprints separadas. Não é um serviço de emergência, não emite alertas oficiais e não recebe relatos públicos verificados.

## O problema que queremos resolver

Depois de um evento crítico, informações sobre problemas e providências podem ficar espalhadas entre conversas, mensagens e anotações. Isso dificulta entender o que aconteceu, acompanhar o que já foi resolvido e identificar o que ainda precisa de atenção.

A ideia do Plano B é oferecer um ponto de organização para essa jornada: **do registro do impacto à recuperação**.

## Como o Plano B foi pensado para funcionar

1. **Registre um impacto:** informe o tipo de ocorrência, bairro, endereço, descrição e severidade.
2. **Acompanhe a situação:** consulte os registros, filtre por tipo ou status e visualize um índice orientativo de prioridade.
3. **Organize a recuperação:** cadastre ações, indique responsáveis e marque tarefas concluídas.
4. **Consulte o histórico:** veja as mudanças de situação para acompanhar a evolução do registro.

O índice de prioridade é uma regra de demonstração do software: **não representa avaliação oficial de risco nem substitui orientação profissional ou de órgãos públicos**.

## Funcionalidades em desenvolvimento

- **Painel de ocorrências:** visão geral dos registros e de suas situações (Sprint Lucas).
- **Cadastro e edição:** criação, consulta, atualização e exclusão de ocorrências (Sprints Lucas e Gustavo).
- **Filtros:** consulta dos registros por tipo e situação (Sprint Lucas).
- **Prioridade orientativa:** cálculo inicial implementado em Java; evolução das regras na Sprint Miguel.
- **Ações de recuperação:** acompanhamento de providências por ocorrência (Sprints Lucas e Gustavo).
- **Histórico:** registro persistente das alterações de situação (Sprint Gustavo).
- **Persistência PostgreSQL:** operações transacionais e integridade entre tabelas (Sprint Gustavo).

### Próximos passos

A evolução prevista inclui uma experiência mais completa de acompanhamento comunitário, melhorias de acessibilidade, relatórios e mecanismos de validação de informações. **Essas melhorias ainda não fazem parte da versão atual.**

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Banco de dados | PostgreSQL 16 |
| Persistência | JDBC com SQL parametrizado |
| Aplicação web | HTTP Server do JDK, HTML e CSS |
| Ambiente local | Docker Compose e Maven |
| Qualidade | Testes de domínio, HTTP e integração com PostgreSQL via GitHub Actions |

A aplicação foi organizada em camadas, separando **domínio**, **serviços**, **persistência** e **interface web**.

## Executar o núcleo Java

Requisitos: **Java 21** e **Maven 3.9+**.

```bash
mvn -q compile exec:java
```

O comando executa uma demonstração de domínio no terminal. **Ainda não inicia uma interface web**: essa entrega faz parte da Sprint de Lucas.

### Testes disponíveis na branch principal

```bash
bash scripts/test-domain.sh
```

Os testes de interface e banco serão reintegrados pelas Pull Requests correspondentes. O PostgreSQL 16 pode ser iniciado com `docker compose up -d postgres`, mas o esquema SQL está sendo implementado na Sprint de Gustavo.

[Acompanhar os testes no GitHub Actions](https://github.com/pitado/planoB/actions).

### Organização das sprints

- [Sprint 1 — Miguel: domínio e regras de negócio](https://github.com/pitado/planoB/issues/1)
- [Sprint 2 — Lucas: interface web e testes HTTP](https://github.com/pitado/planoB/issues/2)
- [Sprint 3 — Gustavo: PostgreSQL, JDBC e integração](https://github.com/pitado/planoB/issues/3)

Confira [o planejamento e os critérios de aceite](docs/sprints.md). A versão anterior do MVP foi preservada na branch [`arquivo/mvp-completo-2026-11-13`](https://github.com/pitado/planoB/tree/arquivo/mvp-completo-2026-11-13), sem atribuir sua autoria retroativamente a integrantes.

## Estrutura do repositório

```text
src/
  main/java/       Regras de negócio, serviços, repositórios e web
  main/resources/  Estilos da interface
  test/java/       Testes automatizados
database/          Esquema SQL e dados fictícios de demonstração
docs/              Arquitetura, diagramas, cronograma e documentação
scripts/           Ferramentas de teste e validação
.github/workflows/ Integração contínua
```

Para conhecer a arquitetura, consulte o [documento de escopo](docs/escopo-e-arquitetura.md), a [modelagem de classes](docs/diagrams/classes.puml) e o [DER planejado](docs/diagrams/der.mmd). As imagens e a documentação final serão geradas após a integração dos módulos.

## Segurança e responsabilidade

O Plano B tem como objetivo **organizar informações e apoiar a preparação e a recuperação**, não substituir a Defesa Civil, os bombeiros, a concessionária de energia ou outras fontes oficiais.

A versão atual utiliza dados fictícios e **não possui autenticação nem autorização suficientes para exposição pública**. Antes de disponibilizá-la para uma comunidade real, será necessário adicionar verificação de relatos, proteção de dados, controles de acesso e procedimentos de moderação.

## Sobre o projeto

O Plano B se inspira em necessidades concretas de continuidade urbana e se relaciona aos **ODS 11 — Cidades e Comunidades Sustentáveis** e **ODS 13 — Ação Climática**.

A documentação de desenvolvimento, a organização do trabalho e os registros específicos da atividade universitária estão na pasta [`docs/`](docs/). O histórico de commits deve refletir as contribuições efetivamente realizadas por cada participante.
