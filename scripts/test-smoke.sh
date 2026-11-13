#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Teste de ponta a ponta: exige PostgreSQL inicializado e Maven.
: "${DB_URL:=jdbc:postgresql://127.0.0.1:5432/plano_b}"
: "${DB_USER:=planob}"
: "${DB_PASSWORD:=planob_dev}"
export DB_URL DB_USER DB_PASSWORD
PORT=18080
export PORT
LOG_FILE="$(mktemp)"
mvn -B -ntp -q compile exec:java > "$LOG_FILE" 2>&1 &
APP_PID=$!
cleanup() {
  kill "$APP_PID" 2>/dev/null || true
  wait "$APP_PID" 2>/dev/null || true
  if [[ "${1:-0}" == "1" ]]; then cat "$LOG_FILE"; fi
  rm -f "$LOG_FILE"
}
trap 'cleanup 0' EXIT

base="http://127.0.0.1:${PORT}"
ready=0
for i in {1..40}; do
  if curl --silent --fail "$base/health" | grep -qx OK; then ready=1; break; fi
  if ! kill -0 "$APP_PID" 2>/dev/null; then break; fi
  sleep 1
done
if [[ "$ready" != "1" ]]; then
  echo "Falha ao iniciar a aplicacao com banco de dados real."
  cleanup 1
  exit 1
fi

curl --fail --silent --show-error "$base/ocorrencias" >/dev/null

header="$(mktemp)"
http_code="$(curl --silent --show-error -D "$header" -o /dev/null -w '%{http_code}' \
  -X POST "$base/ocorrencias" \
  -d 'tipo=VIA_OBSTRUIDA&bairro=Bairro+Teste&logradouro=Rua+do+Teste&descricao=Criada+pelo+smoke+test&severidade=3')"
[[ "$http_code" == "303" ]] || { cat "$header"; exit 1; }
location="$(grep -i '^Location:' "$header" | tr -d '\r' | cut -d' ' -f2)"
rm -f "$header"
[[ "$location" =~ ^/ocorrencias/[0-9]+$ ]] || { echo "Redirect invalido: $location"; exit 1; }
id="${location##*/}"

curl --fail --silent --show-error "$base${location}" | grep -q 'Criada pelo smoke test'
code="$(curl --silent --show-error -o /dev/null -w '%{http_code}' -X POST "$base${location}/atualizar" \
  -d 'tipo=ALAGAMENTO&bairro=Bairro+Teste&logradouro=Rua+do+Teste&descricao=Atualizada+pelo+smoke+test&severidade=4&status=EM_ANALISE')"
[[ "$code" == "303" ]] || { echo "Falha no UPDATE: $code"; exit 1; }
curl --fail --silent --show-error "$base${location}" | grep -q 'Atualizada pelo smoke test'

export PGPASSWORD="$DB_PASSWORD"
count="$(psql -h 127.0.0.1 -U "$DB_USER" -d plano_b -tAc \
  "SELECT COUNT(*) FROM ocorrencias WHERE id=$id AND status='EM_ANALISE'")"
[[ "$count" == "1" ]] || { echo "Atualizacao nao persistiu no PostgreSQL."; exit 1; }

code="$(curl --silent --show-error -o /dev/null -w '%{http_code}' -X POST "$base${location}/excluir" \
  -H 'Content-Type: application/x-www-form-urlencoded' -d '')"
[[ "$code" == "303" ]] || { echo "Falha no DELETE: $code"; exit 1; }
code="$(curl --silent --show-error -o /dev/null -w '%{http_code}' "$base${location}")"
[[ "$code" == "404" ]] || { echo "DELETE nao refletido no GET: $code"; exit 1; }

echo "APROVADO: aplicacao Java iniciou; CRUD HTTP validado contra PostgreSQL real."
