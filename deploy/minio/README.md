# 本机 MinIO 部署

该 Compose 仅运行商品图片对象存储。镜像从 MinIO 官方归档源码 `RELEASE.2025-10-15T17-29-55Z` 构建，不使用第三方浮动镜像。社区版已停止发布新的预编译镜像，后续安全维护需持续评估。

1. 把 `.env.example` 复制为 `.env`，设置两组不同的随机账号与长密码，并把 `MINIO_DATA_DIR` 改为持久磁盘的绝对目录。该目录应允许容器用户 UID `10001` 写入；`.env` 已被 Git 忽略。
2. 在本目录运行 `docker compose --env-file .env -f compose.yml build`，然后运行 `docker compose --env-file .env -f compose.yml up -d`。首次构建需要网络下载 Go 模块，且构建阶段的内存使用不受运行容器的 1 GiB 上限约束。
3. 镜像中同时从固定源码版本构建 `mc` 管理工具。执行下方初始化命令，创建**私有** `product-images` 桶和仅能访问该桶的应用用户。不要把管理账号配置给 Java 服务。
4. 给 Java 服务设置 `MINIO_ENDPOINT=http://127.0.0.1:9000`、`MINIO_BUCKET=product-images`，并把 `.env` 中的 `MINIO_APP_ACCESS_KEY`、`MINIO_APP_SECRET_KEY` 分别配置为 Java 服务的 `MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`。如果 Java 也运行在 Docker 内，应改用同一内部网络的 `http://minio:9000`。配置生效后，用商品管理上传一张测试图，确认开单缩略图和原图预览可用。
5. 备份数据库与 `MINIO_DATA_DIR`，定期在隔离目录验证恢复。单机部署没有跨机器副本；磁盘故障需要从备份恢复。

端口只绑定到本机回环地址。运行时内存上限为 1 GiB，是本项目小图片工作负载的试运行值；上线前查看 `docker stats`、容器重启次数和连续上传表现，再根据实测调整。Docker Desktop 的虚拟机内存和宿主机其他服务也应计入预算。

执行 `docker compose --env-file .env -f compose.yml config` 可检查配置；执行 `docker compose --env-file .env -f compose.yml ps` 可看健康状态。管理员凭据和应用凭据不得写入仓库或日志。

在 `deploy/minio` 目录执行以下初始化命令。命令在容器内读取 `.env` 注入的凭据，避免把密码写在命令行中。首次执行 `mc admin user add` 即创建应用用户；已有用户时跳过该行，避免意外更改其密钥。

```sh
docker compose --env-file .env -f compose.yml exec minio sh -c 'mc alias set local http://127.0.0.1:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"'
docker compose --env-file .env -f compose.yml exec minio mc mb --ignore-existing local/product-images
docker compose --env-file .env -f compose.yml exec minio mc anonymous set none local/product-images
cat product-images-policy.json | docker compose --env-file .env -f compose.yml exec -T minio mc admin policy create local product-images /dev/stdin
docker compose --env-file .env -f compose.yml exec minio sh -c 'mc admin user add local "$MINIO_APP_ACCESS_KEY" "$MINIO_APP_SECRET_KEY"'
docker compose --env-file .env -f compose.yml exec minio sh -c 'mc admin policy attach local product-images --user "$MINIO_APP_ACCESS_KEY"'
```

策略见 `product-images-policy.json`，资源仅限 `product-images` 桶。`s3:GetBucketLocation` 用于 Java SDK 的桶区域探测；仅验证 `mc` 读写不足以覆盖 Java SDK。策略从宿主机文件传入，修正策略时不必重建镜像。完成后使用应用凭据实测上传、读取和删除。上述初始化不属于数据库迁移。

本机首次部署记录（2026-10-21，arm64）：镜像 ID 为 `sha256:f2465338f2e010bad5c842d1cdbe5ef0598034da3160977c081efccde85284f6`；容器健康检查通过，1 GiB 限额下空闲占用约 275 MiB；应用账号完成临时对象的写入、读取与删除。此 ID 仅对应本机此次构建，其他机器重新构建后应记录自己的镜像 ID。

2026-10-26 续接检查：复用上述镜像及已有容器、桶、账号，补齐 Java SDK 所需的桶区域探测权限。临时对象在容器重启后仍可读取，已清理；桶保持私有。Java SDK 与隔离 H2 集成测试已验证原图、160px 缩略图、图片替换、事务回滚清理和删除。检查时容器为 healthy，约占 95 MiB / 1 GiB；此数值是一次轻负载测量，不代表持续并发上传的峰值。

## 已有数据库与后端接入

1. 先备份业务数据库，核对 `base_product` 是否已有 `default_sale_price` 和 `image_object_key`。两列均不存在时，仅手动执行 [`sql/update.sql`](../../sql/update.sql) 中 `2026-10-20 23:54:28 Asia/Shanghai` 的商品字段段落。该文件还包含其他需求的 SQL，不要整体重跑。字段已存在时跳过；只有部分字段存在时，先按实际状态核对，勿重复执行整个 ALTER。全新空库使用 [`sql/grain_oil_ims_init.sql`](../../sql/grain_oil_ims_init.sql)，其末尾已追加两列，不再重复执行升级段。
2. 在 Java 进程的启动环境中配置下表。Compose 的 `.env` 只供 Docker 使用，不会自动传给 IntelliJ 或宿主机 Java 进程。IntelliJ 启动时在当前 Spring Boot Run/Debug Configuration 的环境变量中填写；其他方式启动时在对应服务启动环境中设置。不要把实际密钥写入受 Git 跟踪的配置。

| Java 环境变量 | 本机配置值 |
| --- | --- |
| `MINIO_ENDPOINT` | `http://127.0.0.1:9000` |
| `MINIO_BUCKET` | `product-images` |
| `MINIO_ACCESS_KEY` | 本目录私有 `.env` 的 `MINIO_APP_ACCESS_KEY` |
| `MINIO_SECRET_KEY` | 本目录私有 `.env` 的 `MINIO_APP_SECRET_KEY` |

3. 手动 SQL 和环境变量就绪后，重新启动 Java 服务并发布本次前端。商品管理填写基础单位默认售价并上传主图；销售开单选择该商品，核对缩略图、原图、默认单价、换算单位和手工改价。已有订单与复制订单继续保留原成交价。

本机开发也可将上表四个 `MINIO_*` 属性保存在项目根目录的 `config/application-dev.properties` 中。Spring Boot 在工作目录为项目根目录、启用 `dev` profile 且未覆盖默认配置搜索路径时自动加载该文件；系统环境变量优先。文件已加入 Git 忽略，应设为仅当前用户可读写，不写入管理账号。Compose `.env` 仍不会自动加载；应用账号密钥变更后需同步本地文件并重启后端。缺少凭据会返回 `product-image-storage-unconfigured`。

## 可重复验证

在项目根目录运行 `python3 deploy/minio/verify-storage.py`。脚本从本机私有 `.env` 读取应用凭据，通过子进程环境传给 `ProductImageStorageIntegrationTest`，不把密钥放入命令行或输出。测试只创建内存 H2 商品表和随机对象键，完成后清理测试对象，不连接业务数据库、不执行业务迁移。默认 Maven 测试不运行该用例，只有显式设置 `MINIO_SMOKE_ENABLED=true` 时才启用。

完整业务页面验收和数据库/对象目录的备份恢复演练尚需在上线步骤中手动完成。续接开发未重启正在调试的业务 Java 进程，未修改其运行配置，也未执行业务 SQL。
