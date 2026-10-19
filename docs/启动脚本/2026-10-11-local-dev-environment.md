# 本地开发环境配置记录

记录时间：2026-10-11  
项目：`liangyoudemo`

本文用于后续开发时快速读取本机 Nacos、数据库、Redis、后端和前端代理配置。

## 1. 项目本地服务

### 后端服务

当前项目本地后端端口来自 `src/main/resources/application.yml`：

```yaml
server:
  port: 18088
```

后端基础地址：

```text
http://localhost:18088
```

后端 API 前缀：

```text
/api/v1
```

示例：

```text
http://localhost:18088/api/v1/auth/login
```

### 前端服务

前端目录：

```text
frontend/
```

启动命令：

```bash
cd frontend
npm run serve -- --host 0.0.0.0
```

前端访问地址：

```text
http://localhost:8081/
```

当前 `frontend/.env.development`：

```env
NODE_ENV=development
VUE_APP_BASE_API=/api
VUE_APP_PROXY_TARGET=http://localhost:18088
CHOKIDAR_USEPOLLING=true
```

含义：

- 前端请求 `/api/**`
- Vue dev server 代理到 `http://localhost:18088`
- 最终访问后端 `/api/v1/**`

## 2. Nacos 本地配置

### Nacos 控制台

```text
http://localhost:8848/nacos/
```

### Nacos API 基础地址

```text
http://localhost:8848/nacos/v1
```

### Namespace

当前可访问 namespace：

| namespace | 显示名 | configCount |
|---|---|---:|
| 空字符串 | public | 9 |

查询命令：

```bash
curl -sS 'http://localhost:8848/nacos/v1/console/namespaces'
```

### 当前 Nacos 配置清单

查询命令：

```bash
curl -sS 'http://localhost:8848/nacos/v1/cs/configs?search=blur&dataId=&group=&appName=&config_tags=&pageNo=1&pageSize=50'
```

当前 public namespace / `DEFAULT_GROUP` 下配置：

| dataId | group | type | 主要内容 |
|---|---|---|---|
| `application-dev.yml` | `DEFAULT_GROUP` | yaml | 公共 dev 配置；排除 Druid 自动配置、Feign、Actuator 暴露 |
| `ruoyi-gateway-dev.yml` | `DEFAULT_GROUP` | yaml | RuoYi Gateway 路由、Redis、验证码、白名单、Springdoc |
| `ruoyi-auth-dev.yml` | `DEFAULT_GROUP` | yaml | RuoYi Auth Redis |
| `ruoyi-monitor-dev.yml` | `DEFAULT_GROUP` | yaml | RuoYi Monitor 登录用户、Spring Boot Admin 标题 |
| `ruoyi-system-dev.yml` | `DEFAULT_GROUP` | yaml | RuoYi System Redis、动态数据源、MyBatis、Springdoc |
| `ruoyi-gen-dev.yml` | `DEFAULT_GROUP` | yaml | RuoYi Gen Redis、MySQL、MyBatis、代码生成配置 |
| `ruoyi-job-dev.yml` | `DEFAULT_GROUP` | yaml | RuoYi Job Redis、MySQL、MyBatis、Springdoc |
| `ruoyi-file-dev.yml` | `DEFAULT_GROUP` | yaml | 文件上传、FastDFS、Minio |
| `sentinel-ruoyi-gateway` | `DEFAULT_GROUP` | json | Sentinel Gateway 限流规则 |

### 当前项目期望导入的 Nacos 配置

`src/main/resources/application.yml` 中配置：

```yaml
spring:
  application:
    name: liangyoudemo
  profiles:
    active: dev
  config:
    import:
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?group=DEFAULT_GROUP&refreshEnabled=true
```

因此当前项目会尝试导入：

```text
dataId: liangyoudemo-dev.yml
group: DEFAULT_GROUP
namespace: public
```

当前 Nacos 查询结果：

```text
config data not exist
```

查询命令：

```bash
curl -sS 'http://localhost:8848/nacos/v1/cs/configs?dataId=liangyoudemo-dev.yml&group=DEFAULT_GROUP'
```

结论：

- 当前项目配置导入是 `optional:nacos:...`，所以 `liangyoudemo-dev.yml` 缺失不会阻止后端启动。
- 项目实际会继续使用本地 `src/main/resources/application-dev.yml` 中的 datasource、Redis、JWT 等配置。
- 如果后续希望通过 Nacos 管理本项目配置，应在 Nacos public namespace / `DEFAULT_GROUP` 新增 `liangyoudemo-dev.yml`。

## 3. 当前项目本地数据库配置

来源文件：

- `src/main/resources/application-dev.yml`
- `src/main/resources/application-test.yml`
- `src/main/resources/application-prod.yml`

dev 环境数据库：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/grain_oil_ims?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

拆解：

| 项 | 值 |
|---|---|
| 类型 | MySQL |
| host | `127.0.0.1` |
| port | `3306` |
| database | `grain_oil_ims` |
| username | `root` |
| password | `root` |
| timezone | `Asia/Shanghai` |

本地连接命令：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -proot grain_oil_ims
```

后续如需读取表结构或数据，可优先查询：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -proot -D grain_oil_ims -e 'SHOW TABLES;'
```

## 4. 当前项目本地 Redis 配置

dev 环境 Redis：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password:
      database: 0
```

拆解：

| 项 | 值 |
|---|---|
| host | `127.0.0.1` |
| port | `6379` |
| database | `0` |
| password | 空 |

## 5. Nacos 中其他数据库配置

当前 Nacos 里存在若依相关数据库配置，主要指向：

```text
jdbc:mysql://localhost:3306/ry-cloud
```

涉及 dataId：

- `ruoyi-system-dev.yml`
- `ruoyi-gen-dev.yml`
- `ruoyi-job-dev.yml`

这些配置属于 RuoYi Cloud 示例/相关服务，不是当前 `liangyoudemo` Spring Boot 单体项目的直接 datasource。

## 6. 后续开发读取顺序建议

当需要判断当前项目真实运行配置时，按以下顺序读取：

1. `src/main/resources/application.yml`
2. 当前激活 profile：`spring.profiles.active`
3. `src/main/resources/application-dev.yml`
4. Nacos：`liangyoudemo-dev.yml` / `DEFAULT_GROUP`
5. 如果 Nacos 返回 `config data not exist`，以本地 `application-dev.yml` 为准
6. 前端代理：`frontend/.env.development`

## 7. 常用检查命令

检查 Nacos 是否可访问：

```bash
curl -I http://localhost:8848/nacos/
```

查看 Nacos namespace：

```bash
curl -sS 'http://localhost:8848/nacos/v1/console/namespaces'
```

查看 Nacos 配置列表：

```bash
curl -sS 'http://localhost:8848/nacos/v1/cs/configs?search=blur&dataId=&group=&appName=&config_tags=&pageNo=1&pageSize=50'
```

查看当前项目预期 Nacos 配置：

```bash
curl -sS 'http://localhost:8848/nacos/v1/cs/configs?dataId=liangyoudemo-dev.yml&group=DEFAULT_GROUP'
```

查看前端代理配置：

```bash
sed -n '1,80p' frontend/.env.development
```

查看后端本地配置：

```bash
sed -n '1,220p' src/main/resources/application.yml
sed -n '1,220p' src/main/resources/application-dev.yml
```
