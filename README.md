# Plano B

**Quando a cidade enfrenta uma crise, ter um próximo passo faz a diferença.**

O **Plano B** é uma plataforma de **resiliência urbana e organização comunitária** pensada para ajudar moradores, comunidades e pequenos estabelecimentos a lidar com momentos críticos, como temporais, alagamentos, quedas de energia, danos estruturais e vias bloqueadas.

A proposta nasceu da observação dos impactos de eventos climáticos severos em **Maringá, Paraná**. Em situações assim, os problemas não acontecem isoladamente: uma árvore caída pode bloquear uma rua, uma interrupção de energia pode prejudicar estabelecimentos e uma família pode precisar organizar várias ações antes de retomar sua rotina.

O Plano B busca reunir essas informações de maneira simples e organizada, permitindo **registrar ocorrências, acompanhar sua situação e planejar ações de recuperação**.

> **Estado atual:** o projeto é um protótipo funcional para execução local, com dados de demonstração. Não é um serviço de emergência, não emite alertas oficiais e ainda não recebe relatos públicos verificados.

## O problema que queremos resolver

Depois de um evento crítico, informações sobre problemas e providências podem ficar espalhadas entre conversas, mensagens e anotações. Isso dificulta entender o que aconteceu, acompanhar o que já foi resolvido e identificar o que ainda precisa de atenção.

A ideia do Plano B é oferecer um ponto de organização para essa jornada: **do registro do impacto à recuperação**.

## Como o Plano B funciona

1. **Registre um impacto:** informe o tipo de ocorrência, bairro, endereço, descrição e severidade.
2. **Acompanhe a situação:** consulte os registros, filtre por tipo ou status e visualize um índice orientativo de prioridade.
3. **Organize a recuperação:** cadastre ações, indique responsáveis e marque tarefas concluídas.
4. **Consulte o histórico:** veja as mudanças de situação para acompanhar a evolução do registro.

O índice de prioridade é uma regra de demonstração do software: **não representa avaliação oficial de risco nem substitui orientação profissional ou de órgãos públicos**.

## Funcionalidades disponíveis

- **Painel de ocorrências:** visão geral dos registros e de suas situações.
- **Cadastro e edição:** criação, consulta, atualização e exclusão de ocorrências.
- **Filtros:** busca de registros por tipo e situação.
- **Prioridade orientativa:** cálculo de um índice com base no tipo e na severidade informados.
- **Ações de recuperação:** cadastro de providências associadas a cada ocorrência, com acompanhamento de conclusão.
- **Histórico:** registro das alterações de situação.
- **Persistência real:** dados armazenados em PostgreSQL, com operações transacionais e integridade entre tabelas.

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

## Executar localmente

Requisitos: **Java 21**, **Maven 3.9+** e **Docker com Compose** (ou PostgreSQL 16 configurado).

```bash
docker compose up -d postgres
mvn -q compile exec:java
```

Depois, abra **http://localhost:8080**.

Para utilizar outro banco, configure as variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD`. O arquivo `compose.yaml` contém apenas credenciais de desenvolvimento. Os scripts de inicialização do Docker são executados na primeira criação de um volume novo.

### Testes

```bash
bash scripts/test-domain.sh
bash scripts/test-db.sh
```

O segundo script exige um PostgreSQL em execução e as dependências Maven disponíveis. A integração contínua também testa o fluxo HTTP completo usando um banco real.

[Consultar as execuções do GitHub Actions](https://github.com/pitado/planoB/actions).

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

Para conhecer a arquitetura, consulte o [documento de escopo](docs/escopo-e-arquitetura.md), o [diagrama de classes](docs/diagrams/classes.svg) e o [diagrama de banco de dados](docs/diagrams/der.svg).

## Segurança e responsabilidade

O Plano B tem como objetivo **organizar informações e apoiar a preparação e a recuperação**, não substituir a Defesa Civil, os bombeiros, a concessionária de energia ou outras fontes oficiais.

A versão atual utiliza dados fictícios e **não possui autenticação nem autorização suficientes para exposição pública**. Antes de disponibilizá-la para uma comunidade real, será necessário adicionar verificação de relatos, proteção de dados, controles de acesso e procedimentos de moderação.

## Sobre o projeto

O Plano B se inspira em necessidades concretas de continuidade urbana e se relaciona aos **ODS 11 — Cidades e Comunidades Sustentáveis** e **ODS 13 — Ação Climática**.

A documentação de desenvolvimento, a organização do trabalho e os registros específicos da atividade universitária estão na pasta [`docs/`](docs/). O histórico de commits deve refletir as contribuições efetivamente realizadas por cada participante.
