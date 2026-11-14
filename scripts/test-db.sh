#!/usr/bin/env bash
# Teste de integração JDBC contra PostgreSQL real (requer DB_URL/DB_USER/DB_PASSWORD ou o padrão do compose.yaml).
set -euo pipefail
cd "$(dirname "$0")/.."
DRIVER_VERSION=42.7.5
DRIVER=".build/lib/postgresql-${DRIVER_VERSION}.jar"
if [ ! -f "$DRIVER" ]; then
  mkdir -p .build/lib
  curl -fsSL -o "$DRIVER" "https://repo1.maven.org/maven2/org/postgresql/postgresql/${DRIVER_VERSION}/postgresql-${DRIVER_VERSION}.jar"
fi
mkdir -p .build/classes
find src/main/java src/test/java -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -d .build/classes
java -ea -cp ".build/classes:${DRIVER}" br.com.planob.tests.JdbcIntegrationTests
