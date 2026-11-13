#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Compila domínio + web + testes com Java 21 sem dependências externas.
bash scripts/test-domain.sh
java -ea -cp ".build/classes:src/main/resources" br.com.planob.tests.WebTests
