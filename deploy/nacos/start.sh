#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_BIN="/usr/local/bin/docker"
if [ ! -x "$DOCKER_BIN" ]; then
  DOCKER_BIN="$(command -v docker || true)"
fi
if [ -z "$DOCKER_BIN" ] || [ ! -x "$DOCKER_BIN" ]; then
  echo "未找到 Docker，请先安装 Docker Desktop。" >&2
  exit 1
fi
if [ ! -f "$SCRIPT_DIR/.local/application.properties" ]; then
  echo "缺少 Nacos 私有配置：$SCRIPT_DIR/.local/application.properties" >&2
  exit 1
fi
if ! "$DOCKER_BIN" --context desktop-linux info >/dev/null 2>&1; then
  echo "Docker Desktop 尚未就绪，请先打开 Docker Desktop 后重试。" >&2
  exit 1
fi

# 固定日常启动端口，避免临时验证环境变量影响正式服务。
unset NACOS_HTTP_PORT NACOS_GRPC_PORT
"$DOCKER_BIN" --context desktop-linux compose -f "$SCRIPT_DIR/compose.yml" \
  up -d --wait --wait-timeout 120
echo "Nacos 已就绪：http://localhost:8848/nacos/"
