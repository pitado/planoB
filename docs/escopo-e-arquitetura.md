# Descoberta, concepção e justificativa técnica

## Dor e partes interessadas

Depois de um evento climático severo, moradores, pequenos comércios e organizações de bairro podem ter dificuldade para acompanhar interrupções distintas e planejar a recuperação. Informações dispersas atrapalham a identificação de prioridades e o registro de ações concluídas. O Plano B centraliza **registros acadêmicos/simulados**, classifica ocorrências por tipo e severidade e associa tarefas práticas a cada registro.

**Partes interessadas:** moradores, gestores de pequenos estabelecimentos, lideranças comunitárias e equipes acadêmicas. **Não é** um sistema oficial de Defesa Civil.

## Concepção e ODS

O protótipo está alinhado ao **ODS 11** (cidades inclusivas, seguras, resilientes e sustentáveis) e ao **ODS 13** (ação climática), ao apoiar a organização de informações e o planejamento de recuperação diante de interrupções urbanas. Não alegamos que o produto por si só reduz danos reais sem avaliação em campo.

## Regra de negócio e escopo

Cada ocorrência possui um tipo entre quatro possibilidades, localidade, severidade de 1 a 5, descrição e status. O índice orientativo é calculado por `severidade * 15 + pesoOperacional`, com limite de 100; os pesos variam pelo tipo via herança e sobrescrita. **O valor não é uma previsão de perigo, urgência de atendimento ou orientação pública.**

Uma ocorrência pode ter zero ou muitas ações de recuperação (relacionamento 1:N), com acompanhamento de pendências. O histórico grava abertura e cada alteração de status para fins de rastreabilidade. Excluir a ocorrência elimina ações e histórico vinculados, conforme regra `ON DELETE CASCADE` documentada.

### Requisitos funcionais

Ver tabela RF01–RF08 no README; todos são contemplados pelo escopo do protótipo.

### Requisitos não funcionais

- RNF01: Java 21 e PostgreSQL 16; inicialização reproduzível por Docker Compose.
- RNF02: acesso a banco via JDBC e SQL parametrizado para valores de entrada.
- RNF03: interface HTTP local acessível em navegadores modernos e layout responsivo.
- RNF04: testes de domínio executáveis sem conexão externa; testes JDBC em ambiente isolado.
- RNF05: código dividido em domínio, repositório, serviço, web e configuração.

## Justificativa de escolhas

- **Java:** tipagem estática, classes abstratas e interfaces, herança, `@Override`, alinhado diretamente à rubrica da AEP.
- **PostgreSQL:** persistência relacional de ocorrências, ações e histórico, integridade referencial, transações para atualizar status e guardar o histórico.
- **JDBC:** conexão direta para demonstrar SQL e CRUD real, sem abstrações que escondam a integração exigida.
- **HTTP Server do JDK:** interface web leve sem dependências de framework. Reduz o escopo de infraestrutura, preservando a separação entre regras e apresentação.
- **Arquitetura em camadas:** domínio com regras puras; repositórios de acesso ao banco; web voltado a entrada e visualização. Facilita testes e manutenção.
- **Docker Compose:** reproduz o ambiente de banco em máquinas diferentes e evita instalação manual de PostgreSQL para a demonstração.

## Diagramas

- [Classes UML em PlantUML](diagrams/classes.puml), incluindo herança, polimorfismo, composição 1:N.
- [DER em Mermaid](diagrams/der.mmd), com relacionamentos e chaves.

## Escopo fora do MVP

Sem geolocalização automática, notificações meteorológicas, autenticação, verificação de relatos públicos, despachos de equipes, integração com serviços oficiais ou publicação na internet. Essas funcionalidades exigem requisitos adicionais de segurança e governança.

## Situação acadêmica

Este documento foi elaborado junto ao 2º bimestre. **Não comprova uma entrega no 1º bimestre**, que não ocorreu. O professor deve ser consultado sobre a rubrica de aderência ao planejamento.
