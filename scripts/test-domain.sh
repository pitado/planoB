#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .build/classes
find src/main/java src/test/java -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -d .build/classes
mkdir -p .build/classes/static
cp src/main/resources/static/site.css .build/classes/static/site.css
java -ea -cp .build/classes br.com.planob.tests.DomainTests
java -ea -cp .build/classes br.com.planob.tests.WebTests
