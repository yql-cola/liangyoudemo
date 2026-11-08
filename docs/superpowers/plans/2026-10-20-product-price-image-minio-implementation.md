# 商品默认售价、主图与销售开单选品实施计划

**目标：** 商品维护一个基础单位默认售价和一张主图；开单选品自动带价、可改价，并显示图片；图片保存在本机 Docker MinIO。

**架构：** 沿用商品 CRUD、商品选项、销售开单和权限链。价格仅为表单默认值，订单仍存实际成交价。图片由后端鉴权代理访问私有 MinIO，前端按需取 Blob。

**技术栈：** Spring Boot 3、MyBatis Plus、MySQL、MinIO Java SDK、Vue 2、Element UI、Jest、Docker Compose。

**续接状态（2026-10-26）：** 功能代码和本地验证已完成；保留原工作区的认证、角色权限等其他修改，未创建 commit。业务数据库升级、业务 Java 启动环境及浏览器页面验收仍由上线人员按 `deploy/minio/README.md` 完成。

## 任务 1：商品默认售价与 SQL

**文件：** `sql/update.sql`、`sql/grain_oil_ims_init.sql`、`BaseProduct.java`、`ProductSaveRequest.java`、`ProductResponse.java`、`BasicOptionResponse.java`、`BasicDataService.java`、`BasicDataServiceTest.java`。

- [x] `BasicDataServiceTest` 覆盖商品选项默认价与图片版本；`BasicDataLifecycleIntegrationTest` 覆盖非法金额拒绝、价格保存及清空、普通编辑保留图片引用。
- [x] 在两份 SQL 末尾追加带时间和执行条件的结构变更；商品实体、入参、响应和服务添加 `defaultSalePrice`、`imageObjectKey`。默认价只允许非负、四位小数以内。
- [x] 商品服务、控制器及 H2 集成测试通过。

## 任务 2：商品 Excel 与销售带价

**文件：** `BasicImportService.java`、`ExcelExportService.java`、`BasicDataService.java`、`schemas.js`、`SaleOrderEditor.vue`、`saleOrderForm.js`、相关局部测试。

- [x] 通过真实旧表头导入测试、金额校验测试及销售换算/手工改价测试。
- [x] 商品表单、列表、导入导出追加默认售价；销售选品按基础单位价带入，换算单位用 `convertRate` 计算；未设置价必须明确填写，`0` 保持有效。编辑与复制订单保留成交价。
- [x] 对应 Maven 与 Jest 测试通过。

## 任务 3：MinIO 存储和图片接口

**文件：** `pom.xml`、`application.yml`、新增 `modules/basic/service/ProductImageService.java` 和存储适配、`ProductController.java`、`ResourcePermissionAuthorizationManager.java`、局部测试。

- [x] 覆盖伪造图片、大小/像素限制与权限；新增可选真实 MinIO + H2 测试，验证原图、缩略图、替换、回滚清理和删除。
- [x] 增加配置和固定版本 SDK；实现 JPEG/PNG 上传、160px 缩略图、2MiB/600万像素限制、私有桶存取、原图和缩略图流式响应，以及上传、读取、删除接口。日志仅记商品 ID、对象键、结果及错误类型，不记文件内容或密钥。
- [x] 图片服务、商品控制器、权限映射与真实存储测试通过。

## 任务 4：商品管理和开单图片交互

**文件：** `frontend/src/api/basic/index.js`、`BasicDataPage.vue`、`BasicDataSelect/index.vue`、`SaleOrderEditor.vue`、新增局部图片组件及相关 Jest 测试。

- [x] 主图上传、失败重试、选品缩略图测试通过；续接时先复现换图缓存、观察器重复创建、关闭再打开预览的竞态，再完成修正和回归。
- [x] 商品表单支持暂存、上传、预览、替换、删除；商品列表和开单选项显示缩略图，按需请求、最多 4 个并发；Blob URL 在组件销毁时释放。失败不阻塞价格和订单保存。
- [x] 相关 Jest 测试 48 条及修改文件 lint 通过。

## 任务 5：Docker 部署与交付检查

**文件：** 新增 `deploy/minio/compose.yml`、`deploy/minio/.env.example`、`deploy/minio/README.md`；必要时补充配置示例。

- [x] 编写单机私有桶持久化部署、固定镜像来源、localhost 端口、健康检查、1GiB 试运行内存上限、日志轮转和密钥配置说明。
- [x] 上次已完成源码镜像构建、容器/桶/账号初始化；本次复用已有部署，完成容器重启持久化及 Java SDK 实测，补齐商品图片桶的 `s3:GetBucketLocation`。数据库 SQL 未执行。
- [x] 复核任务相关 diff 和工作区状态，执行局部测试，记录手动上线顺序。不创建 commit。

## 实施注意

- 现有认证模块有工作区改动，本任务不覆盖或提交这些无关改动；共享请求封装仅追加图片和售价错误文案。
- Docker Desktop 已恢复运行；镜像 ID 及运行检查见部署说明。本次没有重复构建、创建桶或创建账号。
- MinIO 社区服务仓库已归档；实施时使用可核验的固定源码/镜像版本，不使用浮动 `latest`。

## 交付验证记录

- `JAVA_TOOL_OPTIONS=-Djava.awt.headless=true mvn -Dtest=BasicDataServiceTest,ProductControllerTest,BasicImportServiceTest,ExcelExportServiceTest,ProductImageServiceTest,ResourcePermissionAuthorizationManagerTest,BasicDataLifecycleIntegrationTest,MasterDataConcurrencyIntegrationTest test`：90 条通过，0 失败、0 错误。
- `python3 deploy/minio/verify-storage.py`：真实 MinIO + 内存 H2 验证通过；测试对象已清理。默认不运行该可选用例。
- `npm run test:unit -- --runInBand --silent tests/unit/product-image.spec.js tests/unit/basic-image-api.spec.js tests/unit/basic-page.spec.js tests/unit/basic-schema.spec.js tests/unit/basic-select.spec.js tests/unit/sale-order-form.spec.js tests/unit/sale-order-ui.spec.js`（`frontend` 目录）：7 组、48 条通过。
- 修改前端文件的 `vue-cli-service lint --no-fix`：通过。
- `npm run build`（`frontend` 目录）：生产构建成功，仍有包体积提示；本次不作无关打包优化。
- `docker compose --env-file .env.example -f compose.yml config --quiet`、策略 JSON/Python 脚本语法检查及 `git diff --check`：通过。
- 未执行全仓库测试。上次扩大的 `BusinessClosureIntegrationTest` 曾出现与本需求无关的固定日期统计断言失败，本次未改动或重复运行该统计用例。
- 未执行业务 SQL、未修改或重启业务 Java 运行配置；真实业务数据库与浏览器完整流程、持续并发上传峰值及备份恢复演练尚未验证。
