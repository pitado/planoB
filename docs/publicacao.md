# Publicar o Plano B no GitHub preservando os commits

O repositório remoto `pitado/plano-b` ainda **não foi criado** quando estes arquivos foram gerados. Não confundir a cópia local com uma publicação concluída.

## Criar o repositório

1. Abrir https://github.com/new com a conta `pitado`.
2. Nome: `plano-b`; descrição sugerida: `Plataforma acadêmica de resiliência urbana — AEP 2026.2`.
3. Selecionar **Public** (as diretrizes da AEP pedem um link público válido).
4. Para enviar o histórico Git sem conflitos, deixar desmarcadas as opções de inicialização com README, `.gitignore` e licença.

## Publicar o histórico real pelo computador

Baixar `Plano-B-Historico.gitbundle`. Em um terminal com Git instalado, na pasta do arquivo, executar:

```bash
git clone Plano-B-Historico.gitbundle plano-b
cd plano-b
git remote remove origin
git remote add origin https://github.com/pitado/plano-b.git
git push -u origin main
```

Este procedimento preserva os commits incrementais, mas **não** comprova participação individual de outros alunos. Os commits gerados na elaboração assistida não devem ser reatribuídos a colegas.

## Alternativa pelo celular

Criar o repositório pelo navegador e enviar o link para continuar a publicação pelas ferramentas conectadas. Até essa ação, não há um repositório remoto criado nem URL verificável.

## Conferência antes de entregar

- Confirmar que o README e os arquivos `/src`, `/database` e `/docs` aparecem no GitHub.
- Acessar a aba **Actions** e confirmar os dois trabalhos de testes; se falharem, corrigir antes da entrega.
- Substituir as referências de "repositório ainda não criado" pelo endereço real e gerar novamente o PDF.
- Preencher nomes, funções e contribuições efetivas dos integrantes.
- Consultar o professor sobre o critério de aderência ao 1º bimestre não entregue.
