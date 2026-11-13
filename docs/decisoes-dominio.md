# Decisões do domínio — Sprint 1

## Contexto

O Plano B acompanha ocorrências fictícias de interrupções urbanas e ações de recuperação. O domínio precisa funcionar sem interface ou banco, para permitir trabalho independente dos demais integrantes.

## Status e ciclo de vida

Cada ocorrência é criada como **ABERTA**. A política `PoliticaTransicaoStatus` define os passos possíveis:

| De | Para | Justificativa |
| --- | --- | --- |
| ABERTA | EM_ANALISE | Primeiro momento de verificação |
| EM_ANALISE | EM_RECUPERACAO | Providências em andamento |
| EM_ANALISE | RESOLVIDA | Caso encerrado sem recuperação |
| EM_RECUPERACAO | RESOLVIDA | Recuperação concluída |
| EM_RECUPERACAO | EM_ANALISE | Reavaliação necessária |
| RESOLVIDA | EM_ANALISE | Reabertura do caso |

Uma atualização sem alterar o status é válida e não deve gerar evento de histórico. **Saltos não listados são rejeitados** com `IllegalArgumentException`. Isso é uma regra de negócio didática, não um protocolo de Defesa Civil.

A validação é executada pelo `OcorrenciaService` antes de chamar o repositório. O repositório JDBC da Sprint 3 registra efetivamente o histórico quando uma transição aceita é persistida; a política não acessa diretamente SQL.

## Índice orientativo

O cálculo usa `min(100, severidade × 15 + pesoOperacional())`. O peso é especializado nas quatro subclasses de `Ocorrencia` usando `@Override`. O método `explicarIndiceImpacto()` apresenta a conta e a faixa classificatória, com aviso explícito sobre seu caráter **ilustrativo**.

Os pesos foram definidos para demonstrar polimorfismo; **não foram calibrados, validados em campo nem aprovados por órgão técnico**. Não representam risco real, urgência de atendimento ou alertas oficiais.

## Testes e contratos

`bash scripts/test-domain.sh` compila com Java 21 e executa os testes do domínio usando implementações de memória exclusivas de teste. Os casos verificam transições aceitas e recusadas, imutabilidade da ocorrência após rejeição, histórico sem alterações indevidas, polimorfismo, validação de campos e explicação do cálculo.

O serviço mantém o contrato de métodos usado pela futura Sprint 2, sem introduzir dependências de HTTP. A Sprint 3 deve implementar os contratos de repositório e testar a persistência real.

## Autoria e integração

Implementação assistida por IA, realizada na branch `sprint/miguel-dominio` a pedido do integrante responsável pelo módulo. A revisão, domínio do código e apresentação devem ser realizados pelos membros que efetivamente participarem. O histórico preserva a origem dos commits; não simula trabalho de terceiros.
