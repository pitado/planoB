# Persistência PostgreSQL — Sprint 3

| Arquivo | Função |
| --- | --- |
| `schema.sql` | Tabelas `ocorrencias`, `acoes_recuperacao`, `historico_status`, chaves estrangeiras, `CHECK`s e índices. Idempotente. |
| `seed.sql` | Dados **fictícios** de demonstração, com histórico coerente. Aplicar apenas em banco vazio. |

## Inicialização

```bash
docker compose up -d postgres      # na primeira subida aplica schema.sql e seed.sql
docker compose down -v             # apaga o volume para recomeçar do zero
```

Aplicação manual em um banco existente:

```bash
psql "postgresql://planob:planob_dev@localhost:5432/plano_b" -f database/schema.sql -f database/seed.sql
```

Variáveis de conexão: `DB_URL`, `DB_USER`, `DB_PASSWORD` (ver `.env.example`).

## Decisões

- `ON DELETE CASCADE` em ações e histórico: excluir uma ocorrência remove seus dependentes.
- `status_anterior` é `NULL` apenas no registro de criação; `ck_historico_mudanca` impede registrar "mudança" sem mudança.
- O histórico é gravado na **mesma transação** do `INSERT`/`UPDATE` da ocorrência (`JdbcOcorrenciaRepository`), com `SELECT ... FOR UPDATE` para evitar histórico inconsistente em concorrência.
- Os valores de `tipo` e `status` espelham os enums Java; novos valores exigem alterar o `CHECK` e o enum juntos.
