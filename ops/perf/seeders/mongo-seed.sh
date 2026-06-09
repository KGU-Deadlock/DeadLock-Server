#!/bin/bash
# Restores a pre-baked MongoDB dataset into the running mongo instance.
# No-op when DATASET is empty or the restore directory is not found.
set -e

if [ -z "$DATASET" ]; then
  echo "[mongo-seeder] DATASET not set ??skipping restore."
  exit 0
fi

RESTORE_DIR="/perf-datasets/${DATASET}/mongo"

if [ ! -d "$RESTORE_DIR" ]; then
  echo "[mongo-seeder] Restore dir not found at ${RESTORE_DIR} ??skipping."
  exit 0
fi

echo "[mongo-seeder] Restoring dataset '${DATASET}' from ${RESTORE_DIR} ..."
mongorestore --drop --uri="mongodb://mongo:27017" --dir="$RESTORE_DIR"
echo "[mongo-seeder] MongoDB dataset '${DATASET}' restored successfully."
