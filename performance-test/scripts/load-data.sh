#!/bin/bash

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <seed-directory>"
  exit 1
fi

DB_HOST=${DB_HOST:-127.0.0.1}
DB_PORT=${DB_PORT:-3506}
DB_NAME=${DB_NAME:-fillinv}
DB_USER=${DB_USER:-root}
DB_PASSWORD=${DB_PASSWORD:-root}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SEED_DIR="${SCRIPT_DIR}/../seeds/$1"

if [ ! -d "$SEED_DIR" ]; then
  echo "Seed directory not found: $SEED_DIR"
  exit 1
fi

echo "===================================="
echo "Loading seed data"
echo "Scenario : $1"
echo "Directory: $SEED_DIR"
echo "===================================="

export MYSQL_PWD=$DB_PASSWORD

for file in $(find "$SEED_DIR" -name "*.sql" | sort); do
  echo "Executing $(basename "$file")"

  mysql \
    -h "$DB_HOST" \
    -P "$DB_PORT" \
    -u "$DB_USER" \
    "$DB_NAME" < "$file"

  echo "Completed $(basename "$file")"
done

echo "===================================="
echo "Seed data loaded successfully"
echo "===================================="