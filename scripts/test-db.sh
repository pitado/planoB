#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-domain.sh
mvn -B -ntp -q -DskipTests dependency:copy-dependencies -DoutputDirectory=target/dependency
java -ea -cp ".build/classes:target/dependency/*" br.com.planob.tests.JdbcIntegrationTests
