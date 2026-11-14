#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Testes persistentes: exigem PostgreSQL 16 e Java 21.
# Cria classes e obtém driver JDBC se ainda não estiverem disponíveis.
if [ ! -f ".build/lib/postgresql-42.7.5.jar" ]; then
  bash scripts/test-db.sh
fi
mkdir -p .build/classes
find src/main/java src/test/java -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -d .build/classes
java -ea -cp ".build/classes:.build/lib/postgresql-42.7.5.jar:src/main/resources" br.com.planob.tests.WebDbTests
