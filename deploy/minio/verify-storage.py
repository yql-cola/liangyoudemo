#!/usr/bin/env python3
"""Exercise Java image storage against local MinIO and an isolated H2 database."""
import os
from pathlib import Path
import subprocess


def main():
    project_root = Path(__file__).resolve().parents[2]
    settings_file = Path(__file__).with_name('.env')
    if not settings_file.is_file():
        raise SystemExit('Missing deploy/minio/.env; configure local MinIO first.')
    settings = dict(line.split('=', 1) for line in settings_file.read_text().splitlines()
                    if line and not line.startswith('#') and '=' in line)
    if not all(settings.get(key) for key in ('MINIO_APP_ACCESS_KEY', 'MINIO_APP_SECRET_KEY')):
        raise SystemExit('Missing application credentials in deploy/minio/.env.')
    # Pass credentials through the child environment, never command arguments or output.
    test_env = os.environ.copy()
    test_env.update({
        'JAVA_TOOL_OPTIONS': (test_env.get('JAVA_TOOL_OPTIONS', '') + ' -Djava.awt.headless=true').strip(),
        'MINIO_SMOKE_ENABLED': 'true',
        'MINIO_ENDPOINT': 'http://127.0.0.1:9000',
        'MINIO_BUCKET': 'product-images',
        'MINIO_ACCESS_KEY': settings['MINIO_APP_ACCESS_KEY'],
        'MINIO_SECRET_KEY': settings['MINIO_APP_SECRET_KEY'],
    })
    return subprocess.run(['mvn', '-q', '-Dtest=ProductImageStorageIntegrationTest', 'test'],
                          cwd=project_root, env=test_env).returncode


if __name__ == '__main__':
    raise SystemExit(main())
