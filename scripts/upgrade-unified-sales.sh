#!/usr/bin/env bash
# Run after v1.1.0 upgrade against an empty sales dataset.
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
for argument in "$@"; do
  case "$argument" in -f|--force|--force=*) echo 'Refusing --force; stop on SQL errors.' >&2; exit 2;; esac
done
mysql "$@" --default-character-set=utf8mb4 --skip-force < sql/grain_oil_ims_patch_2026_10_20_unified_sales.sql
echo 'Unified sales schema and permissions upgraded.'
