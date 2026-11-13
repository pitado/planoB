#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for name in classes der; do
  dot -Tsvg "docs/diagrams/$name.dot" -o "docs/diagrams/$name.svg"
  dot -Tpng -Gdpi=180 "docs/diagrams/$name.dot" -o "docs/diagrams/$name.png"
done
