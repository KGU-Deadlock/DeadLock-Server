#!/bin/sh
# Copies a pre-baked Redis dump.rdb into the data volume before redis starts.
# No-op when DATASET is empty or the rdb file is not found.
set -e

if [ -z "$DATASET" ]; then
  echo "[redis-seeder] DATASET not set ??starting Redis with empty data."
  exit 0
fi

SRC="/perf-datasets/${DATASET}/redis/dump.rdb"
DST="/data/dump.rdb"

if [ ! -f "$SRC" ]; then
  echo "[redis-seeder] dump.rdb not found at ${SRC} ??starting Redis with empty data."
  exit 0
fi

echo "[redis-seeder] Copying dump.rdb from dataset '${DATASET}' ..."
cp "$SRC" "$DST"
echo "[redis-seeder] dump.rdb ready at ${DST}."
