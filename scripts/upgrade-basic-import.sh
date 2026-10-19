#!/usr/bin/env bash
# Add import permissions to an installation that has already run the v1.1.0 upgrade.
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

for argument in "$@"; do
  case "$argument" in
    -f|--force|--force=*) echo 'Refusing --force: stop on the first SQL error.' >&2; exit 2 ;;
  esac
done

mysql "$@" --default-character-set=utf8mb4 --skip-force grain_oil_ims < sql/grain_oil_ims_patch_2026_10_19_basic_import_permissions.sql
echo 'Basic-data import permissions upgraded successfully.'
