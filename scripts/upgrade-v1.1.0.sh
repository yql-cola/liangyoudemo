#!/usr/bin/env bash
# Supply ordinary mysql connection arguments, e.g. --login-path=liangyou-migration.
# All SQL targets grain_oil_ims; review USE statements before changing database names.
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"

for argument in "$@"; do
  case "$argument" in
    -f|--force|--force=*)
      echo 'Refusing --force: an upgrade must stop at the first SQL error.' >&2
      exit 2
      ;;
  esac
done

# A single batch avoids SOURCE swallowing errors and prompts for the password once.
# mysql defaults to aborting a batch at the first error; do not use --force.
cat sql/grain_oil_ims_patch_2026_04_16_soft_delete_columns.sql \
    sql/grain_oil_ims_upgrade_v1_1_0.sql \
    sql/grain_oil_ims_patch_2026_10_17_basic_data_constraints.sql \
    sql/grain_oil_ims_patch_2026_10_17_permissions.sql \
    sql/grain_oil_ims_patch_2026_10_19_basic_import_permissions.sql \
  | mysql "$@" --default-character-set=utf8mb4 --skip-force

echo 'v1.1.0 schema and permissions upgraded successfully.'
