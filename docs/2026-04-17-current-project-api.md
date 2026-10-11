# 当前项目 API 文档

## 文档信息

- 项目：米面粮油店进销存与记账管理系统
- 生成日期：2026-04-17
- 基础路径：`/api/v1`
- 代码对齐范围：`src/main/java/org/liangyou/modules`
- 输出目标：开发版 / 可直接联调级

## 代码上下文对齐结果

- 已有模块：
  - `auth`
  - `user`
  - `purchase`
  - `sale`
  - `inventory`
  - `statistics`
- 已有接口：
  - `AuthController`
  - `UserController`
  - `RoleController`
  - `MenuController`
  - `PurchaseInController`
  - `SaleOutController`
  - `InventoryController`
  - `StatisticsController`
- 已有实体：
  - `BaseProduct`
  - `BizPurchaseIn` / `BizPurchaseInItem`
  - `BizSaleOut` / `BizSaleOutItem`
  - `InvStock` / `InvBatch` / `InvStockFlow`
  - `SysUser` / `SysRole` / `SysPermission`
- 已有鉴权方式：
  - JWT
  - `Authorization: Bearer {token}`
- 已有返回结构：
  - `ApiResponse{code,message,data}`
  - `PageResponse{list,total}`
- 已有错误码风格：
  - 业务错误码放入 `ApiResponse.code`
  - 统一异常处理返回 `400/401/404`
- 已有分页风格：
  - `pageNum/pageSize`

## 差异说明

- 代码现状：
  - 当前项目未发现独立账务模块接口
  - 当前项目未发现商品主数据独立 Controller
  - 当前项目未发现统一排序参数
- 目标要求：
  - 用户希望文档同时支撑当前项目开发与后续独立账务扩展
- 冲突点：
  - 若直接生成账务 API，会与代码现状冲突
- 建议处理方式：
  - 本文档严格只覆盖当前已实现接口
  - 独立账务模块只在 PRD 中作为扩展设计说明，不在 API 中伪造未实现接口

## 通用规范

### 认证方式

- 请求头：`Authorization: Bearer {token}`
- 鉴权机制：JWT
- 权限模型：RBAC
- 统一 Content-Type：`application/json`
- 全局扩展请求头：当前在代码中仅发现可配置全局头能力，未发现固定启用的额外请求头

### 公共请求头

| 字段名 | 类型 | 必填 | 示例 | 说明 |
| --- | --- | --- | --- | --- |
| Authorization | String | 除登录外是 | `Bearer eyJhbGci...` | JWT 鉴权头 |
| X-Trace-Id | String | 否 | 无固定示例 | 当前代码未发现固定启用 |
| X-Timestamp | String | 否 | 无固定示例 | 当前代码未发现固定启用 |
| X-Nonce | String | 否 | 无固定示例 | 当前代码未发现固定启用 |
| X-Signature | String | 否 | 无固定示例 | 当前代码未发现固定启用 |

说明：
- `OpenApiConfig` 支持从配置中追加全局头，但当前代码未明确给出固定头清单。
- 因此联调时默认只要求 `Authorization`。

### 统一成功响应

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| code | Integer | 是 | 业务状态码，成功固定为 `0` |
| message | String | 是 | 成功固定为 `success` |
| data | Object/Array/Number/Null | 是 | 业务数据体 |

成功示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 统一错误响应

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| code | Integer | 是 | 业务错误码，当前项目常见为 `400/401/404` |
| message | String | 是 | 错误消息或校验短码 |
| data | Null | 是 | 固定为 `null` |

错误示例：

```json
{
  "code": 400,
  "message": "qty-required",
  "data": null
}
```

### 分页响应结构

`data` 为分页对象时字段如下：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| list | Array | 是 | 当前页数据列表 |
| total | Long | 是 | 总记录数 |

分页示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 28
  }
}
```

### 分页规范

- 分页参数：`pageNum/pageSize`
- 默认值：`pageNum=1`、`pageSize=10`
- 建议最大页大小：`100`

### 通用错误码

| 错误码 | HTTP 状态 | 含义 |
| --- | --- | --- |
| 400 | 400 | 参数校验失败或业务错误 |
| 401 | 401 | 未登录或 Token 无效 |
| 404 | 400 | 业务资源不存在，当前项目通过业务异常返回 |

### 当前项目已识别错误消息

- `username-required`
- `password-required`
- `warehouse-id-required`
- `supplier-id-required`
- `customer-id-required`
- `biz-date-required`
- `items-required`
- `product-id-required`
- `unit-id-required`
- `qty-required`
- `unit-price-required`
- `amount-required`
- `batch-no-required`
- `inbound-time-required`
- `total-qty-required`
- `available-qty-required`
- `purchase-in-not-found`
- `sale-out-not-found`
- `inventory-not-found`
- `insufficient-stock`
- `purchase-in-already-consumed`
- `inventory-rollback-conflict`
- `username-exists`
- `role-not-found`
- `permission-not-found`
- `role-disabled`
- `permission-disabled`
- `role-code-exists`
- `未登录或 Token 无效`

## 1. 认证接口

### 1.1 用户登录

- 接口名称：用户登录
- URL：`POST /api/v1/auth/login`
- 权限：匿名可访问

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| token | String | 是 | JWT Token |
| tokenType | String | 是 | Token 类型，固定 `Bearer` |
| expiresIn | Long | 是 | Token 有效期秒数 |
| userInfo | Object | 是 | 当前登录用户信息 |
| userInfo.id | Long | 是 | 用户 ID |
| userInfo.username | String | 是 | 用户名 |
| userInfo.realName | String | 是 | 真实姓名 |
| userInfo.roles | Array<String> | 是 | 角色编码列表 |
| userInfo.permissions | Array<String> | 是 | 权限编码列表 |

成功示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "id": 100,
      "username": "warehouse01",
      "realName": "仓库员工A",
      "roles": ["ACCOUNTANT"],
      "permissions": ["purchase:order:list", "sale:order:create"]
    }
  }
}
```

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 用户名为空 | 400 | `username-required` |
| 密码为空 | 400 | `password-required` |
| 用户名或密码错误 | 400 | `用户名或密码错误` |

### 1.2 获取当前登录用户信息

- 接口名称：获取当前登录用户信息
- URL：`GET /api/v1/auth/me`
- 权限：已登录

请求参数：无

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户 ID |
| username | String | 是 | 用户名 |
| realName | String | 是 | 真实姓名 |
| roles | Array<String> | 是 | 角色编码列表 |
| permissions | Array<String> | 是 | 权限编码列表 |

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 未登录或 Token 无效 | 401 | `未登录或 Token 无效` |

### 1.3 退出登录

- 接口名称：退出登录
- URL：`POST /api/v1/auth/logout`
- 权限：已登录

请求头字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| Authorization | String | 否 | Bearer Token，控制器允许为空但业务上应传 |

成功响应：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| data | Null | 是 | 固定为 `null` |

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| Token 无效 | 401 | `未登录或 Token 无效` |

## 2. 用户与权限接口

### 2.1 新增用户

- URL：`POST /api/v1/users`
- 权限：`system:user:create`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | String | 是 | 登录用户名 |
| password | String | 是 | 登录密码 |
| realName | String | 是 | 真实姓名 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| warehouseId | Long | 是 | 所属仓库 ID |
| status | Integer | 是 | 状态，`1` 启用 `0` 停用 |
| isSuperAdmin | Integer | 否 | 是否超级管理员，默认 `0` |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| data | Long | 是 | 新创建用户 ID |

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 用户名为空 | 400 | `username-required` |
| 密码为空 | 400 | `password-required` |
| 真实姓名为空 | 400 | `real-name-required` |
| 仓库为空 | 400 | `warehouse-id-required` |
| 状态为空 | 400 | `status-required` |
| 用户名重复 | 400 | `username-exists` |

### 2.2 修改用户

- URL：`PUT /api/v1/users/{id}`
- 权限：`system:user:update`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户 ID |

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| realName | String | 是 | 真实姓名 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| warehouseId | Long | 是 | 所属仓库 ID |
| status | Integer | 是 | 状态 |
| isSuperAdmin | Integer | 否 | 是否超级管理员 |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 用户不存在 | 404 | `user-not-found` |
| 真实姓名为空 | 400 | `real-name-required` |
| 仓库为空 | 400 | `warehouse-id-required` |
| 状态为空 | 400 | `status-required` |

### 2.3 删除用户

- URL：`DELETE /api/v1/users/{id}`
- 权限：`system:user:delete`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户 ID |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 用户不存在 | 404 | `user-not-found` |

### 2.4 查询用户详情

- URL：`GET /api/v1/users/{id}`
- 权限：`system:user:view`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户 ID |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户 ID |
| username | String | 是 | 用户名 |
| realName | String | 是 | 真实姓名 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| warehouseId | Long | 是 | 仓库 ID |
| status | Integer | 是 | 状态 |
| isSuperAdmin | Integer | 是 | 是否超级管理员 |
| roleIds | Array<Long> | 是 | 绑定角色 ID 列表 |

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 用户不存在 | 404 | `user-not-found` |

### 2.5 分页查询用户

- URL：`GET /api/v1/users`
- 权限：`system:user:list`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| pageNum | Long | 否 | 页码，默认 `1` |
| pageSize | Long | 否 | 每页条数，默认 `10` |
| username | String | 否 | 用户名模糊筛选 |
| realName | String | 否 | 真实姓名筛选 |
| warehouseId | Long | 否 | 仓库 ID |
| status | Integer | 否 | 状态 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| list | Array<Object> | 是 | 用户列表 |
| list[].id | Long | 是 | 用户 ID |
| list[].username | String | 是 | 用户名 |
| list[].realName | String | 是 | 真实姓名 |
| list[].phone | String | 否 | 手机号 |
| list[].email | String | 否 | 邮箱 |
| list[].warehouseId | Long | 是 | 仓库 ID |
| list[].status | Integer | 是 | 状态 |
| list[].isSuperAdmin | Integer | 是 | 是否超级管理员 |
| list[].roleIds | Array<Long> | 是 | 角色 ID 列表 |
| total | Long | 是 | 总记录数 |

### 2.6 给用户分配角色

- URL：`POST /api/v1/users/{id}/roles`
- 权限：`system:user:grant-role`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户 ID |

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| roleIds | Array<Long> | 是 | 角色 ID 列表，覆盖式更新 |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 用户不存在 | 404 | `user-not-found` |
| 角色不存在 | 404 | `role-not-found` |
| 角色被禁用 | 400 | `role-disabled` |

### 2.7 给用户单独授权

- URL：`POST /api/v1/users/{id}/permissions`
- 权限：`system:user:grant-permission`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户 ID |

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| grantPermissionIds | Array<Long> | 否 | 直接授予的权限 ID 列表 |
| revokePermissionIds | Array<Long> | 否 | 直接收回的权限 ID 列表 |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 权限冲突 | 400 | `permission-conflict` |
| 权限不存在 | 404 | `permission-not-found` |
| 权限被禁用 | 400 | `permission-disabled` |

### 2.8 查询用户最终权限

- URL：`GET /api/v1/users/{id}/permissions`
- 权限：`system:user:view-permission`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户 ID |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | Long | 是 | 用户 ID |
| roleIds | Array<Long> | 是 | 角色 ID 列表 |
| permissionIds | Array<Long> | 是 | 最终权限 ID 列表 |

### 2.9 新增角色

- URL：`POST /api/v1/roles`
- 权限：`system:role:create`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| roleCode | String | 是 | 角色编码 |
| roleName | String | 是 | 角色名称 |
| status | Integer | 否 | 状态，默认 `1` |
| remark | String | 否 | 备注 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 角色 ID |
| roleCode | String | 是 | 角色编码 |
| roleName | String | 是 | 角色名称 |
| status | Integer | 是 | 状态 |
| remark | String | 否 | 备注 |

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 角色编码为空 | 400 | `role-code-required` |
| 角色名称为空 | 400 | `role-name-required` |
| 角色编码重复 | 400 | `role-code-exists` |

### 2.10 修改角色

- URL：`PUT /api/v1/roles/{id}`
- 权限：`system:role:update`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 角色 ID |

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| roleCode | String | 是 | 角色编码 |
| roleName | String | 是 | 角色名称 |
| status | Integer | 是 | 状态 |
| remark | String | 否 | 备注 |

成功响应 `data` 字段：同新增角色

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 角色不存在 | 404 | `role-not-found` |
| 角色编码为空 | 400 | `role-code-required` |
| 角色名称为空 | 400 | `role-name-required` |
| 状态为空 | 400 | `status-required` |

### 2.11 删除角色

- URL：`DELETE /api/v1/roles/{id}`
- 权限：`system:role:delete`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 角色 ID |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 角色不存在 | 404 | `role-not-found` |

### 2.12 查询角色列表

- URL：`GET /api/v1/roles`
- 权限：`system:role:list`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| roleName | String | 否 | 角色名称 |
| roleCode | String | 否 | 角色编码 |
| status | Integer | 否 | 状态 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| [] | Array<Object> | 是 | 角色数组 |
| [][].id | Long | 是 | 角色 ID |
| [][].roleCode | String | 是 | 角色编码 |
| [][].roleName | String | 是 | 角色名称 |
| [][].status | Integer | 是 | 状态 |
| [][].remark | String | 否 | 备注 |

### 2.13 给角色分配权限

- URL：`POST /api/v1/roles/{id}/permissions`
- 权限：`system:role:grant`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 角色 ID |

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| permissionIds | Array<Long> | 是 | 权限 ID 列表，覆盖式更新 |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 角色不存在 | 404 | `role-not-found` |
| 权限不存在 | 404 | `permission-not-found` |
| 权限被禁用 | 400 | `permission-disabled` |

### 2.14 查询菜单树

- URL：`GET /api/v1/menus/tree`
- 权限：`system:menu:view`

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| [] | Array<Object> | 是 | 菜单树数组 |
| [][].id | Long | 是 | 菜单 ID |
| [][].name | String | 是 | 菜单名称 |
| [][].path | String | 是 | 前端路由 |
| [][].children | Array<Object> | 是 | 子菜单列表，递归结构 |

## 3. 采购入库接口

### 3.1 新增采购入库单

- URL：`POST /api/v1/purchase-ins`
- 权限：`purchase:order:create`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 是 | 仓库 ID |
| supplierId | Long | 是 | 供应商 ID |
| bizDate | String(LocalDate) | 是 | 业务日期 |
| status | Integer | 否 | 状态，默认 `1` |
| remark | String | 否 | 备注 |
| items | Array<Object> | 是 | 明细列表 |
| items[].productId | Long | 是 | 商品 ID |
| items[].unitId | Long | 是 | 单位 ID |
| items[].qty | BigDecimal | 是 | 数量 |
| items[].unitPrice | BigDecimal | 是 | 单价 |
| items[].amount | BigDecimal | 是 | 金额 |
| items[].batchNo | String | 是 | 批次号 |
| items[].productionDate | String(LocalDate) | 否 | 生产日期 |
| items[].expiryDate | String(LocalDate) | 否 | 过期日期 |
| items[].inboundTime | String(LocalDateTime) | 是 | 入库时间 |
| items[].remark | String | 否 | 明细备注 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| data | Long | 是 | 新建采购单 ID |

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 仓库为空 | 400 | `warehouse-id-required` |
| 供应商为空 | 400 | `supplier-id-required` |
| 业务日期为空 | 400 | `biz-date-required` |
| 明细为空 | 400 | `items-required` |
| 商品为空 | 400 | `product-id-required` |
| 批次号为空 | 400 | `batch-no-required` |

### 3.2 修改采购入库单

- URL：`PUT /api/v1/purchase-ins/{id}`
- 权限：`purchase:order:update`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 采购单 ID |

请求体字段：同 `3.1`

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 采购单不存在 | 404 | `purchase-in-not-found` |
| 采购单已被消耗 | 400 | `purchase-in-already-consumed` |
| 库存回滚冲突 | 400 | `inventory-rollback-conflict` |

### 3.3 分页查询采购入库单

- URL：`GET /api/v1/purchase-ins`
- 权限：`purchase:order:list`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| pageNum | Long | 否 | 页码 |
| pageSize | Long | 否 | 每页条数 |
| orderNo | String | 否 | 入库单号 |
| warehouseId | Long | 否 | 仓库 ID |
| supplierId | Long | 否 | 供应商 ID |
| status | Integer | 否 | 状态 |
| bizDateFrom | String(LocalDate) | 否 | 业务开始日期 |
| bizDateTo | String(LocalDate) | 否 | 业务结束日期 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| list | Array<Object> | 是 | 采购单列表 |
| list[].id | Long | 是 | 采购单 ID |
| list[].orderNo | String | 是 | 入库单号 |
| list[].warehouseId | Long | 是 | 仓库 ID |
| list[].supplierId | Long | 是 | 供应商 ID |
| list[].bizDate | String(LocalDate) | 是 | 业务日期 |
| list[].totalAmount | BigDecimal | 是 | 总金额 |
| list[].status | Integer | 是 | 状态 |
| list[].remark | String | 否 | 备注 |
| list[].items | Array<Object> | 是 | 明细列表 |
| list[].items[].id | Long | 是 | 明细 ID |
| list[].items[].productId | Long | 是 | 商品 ID |
| list[].items[].unitId | Long | 是 | 单位 ID |
| list[].items[].qty | BigDecimal | 是 | 数量 |
| list[].items[].unitPrice | BigDecimal | 是 | 单价 |
| list[].items[].amount | BigDecimal | 是 | 金额 |
| list[].items[].batchNo | String | 是 | 批次号 |
| list[].items[].productionDate | String(LocalDate) | 否 | 生产日期 |
| list[].items[].expiryDate | String(LocalDate) | 否 | 过期日期 |
| list[].items[].inboundTime | String(LocalDateTime) | 是 | 入库时间 |
| list[].items[].remark | String | 否 | 备注 |
| total | Long | 是 | 总记录数 |

### 3.4 查询采购入库单详情

- URL：`GET /api/v1/purchase-ins/{id}`
- 权限：`purchase:order:view`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 采购单 ID |

成功响应 `data` 字段：同 `3.3 list[]` 单项字段

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 采购单不存在 | 404 | `purchase-in-not-found` |

### 3.5 删除采购入库单

- URL：`DELETE /api/v1/purchase-ins/{id}`
- 权限：`purchase:order:delete`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 采购单 ID |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 采购单不存在 | 404 | `purchase-in-not-found` |
| 采购单已被消耗 | 400 | `purchase-in-already-consumed` |
| 库存回滚冲突 | 400 | `inventory-rollback-conflict` |

## 4. 销售出库接口

### 4.1 新增销售出库单

- URL：`POST /api/v1/sale-outs`
- 权限：`sale:order:create`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 是 | 仓库 ID |
| customerId | Long | 是 | 客户 ID |
| bizDate | String(LocalDate) | 是 | 业务日期 |
| status | Integer | 否 | 状态，默认 `1` |
| remark | String | 否 | 备注 |
| items | Array<Object> | 是 | 明细列表 |
| items[].productId | Long | 是 | 商品 ID |
| items[].unitId | Long | 是 | 单位 ID |
| items[].qty | BigDecimal | 是 | 数量 |
| items[].unitPrice | BigDecimal | 是 | 销售单价 |
| items[].amount | BigDecimal | 是 | 金额 |
| items[].remark | String | 否 | 备注 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| data | Long | 是 | 新建销售单 ID |

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 仓库为空 | 400 | `warehouse-id-required` |
| 客户为空 | 400 | `customer-id-required` |
| 业务日期为空 | 400 | `biz-date-required` |
| 明细为空 | 400 | `items-required` |
| 库存不足 | 400 | `insufficient-stock` |

### 4.2 修改销售出库单

- URL：`PUT /api/v1/sale-outs/{id}`
- 权限：`sale:order:update`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 销售单 ID |

请求体字段：同 `4.1`

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 销售单不存在 | 404 | `sale-out-not-found` |
| 库存不足 | 400 | `insufficient-stock` |
| 库存回滚冲突 | 400 | `inventory-rollback-conflict` |

### 4.3 分页查询销售出库单

- URL：`GET /api/v1/sale-outs`
- 权限：`sale:order:list`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| pageNum | Long | 否 | 页码 |
| pageSize | Long | 否 | 每页条数 |
| orderNo | String | 否 | 出库单号 |
| warehouseId | Long | 否 | 仓库 ID |
| customerId | Long | 否 | 客户 ID |
| status | Integer | 否 | 状态 |
| bizDateFrom | String(LocalDate) | 否 | 开始日期 |
| bizDateTo | String(LocalDate) | 否 | 结束日期 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| list | Array<Object> | 是 | 销售单列表 |
| list[].id | Long | 是 | 销售单 ID |
| list[].orderNo | String | 是 | 出库单号 |
| list[].warehouseId | Long | 是 | 仓库 ID |
| list[].customerId | Long | 是 | 客户 ID |
| list[].bizDate | String(LocalDate) | 是 | 业务日期 |
| list[].totalAmount | BigDecimal | 是 | 总金额 |
| list[].status | Integer | 是 | 状态 |
| list[].remark | String | 否 | 备注 |
| list[].items | Array<Object> | 是 | 明细列表 |
| list[].items[].id | Long | 是 | 明细 ID |
| list[].items[].productId | Long | 是 | 商品 ID |
| list[].items[].unitId | Long | 是 | 单位 ID |
| list[].items[].qty | BigDecimal | 是 | 数量 |
| list[].items[].unitPrice | BigDecimal | 是 | 销售单价 |
| list[].items[].amount | BigDecimal | 是 | 金额 |
| list[].items[].remark | String | 否 | 备注 |
| total | Long | 是 | 总记录数 |

### 4.4 查询销售出库单详情

- URL：`GET /api/v1/sale-outs/{id}`
- 权限：`sale:order:view`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 销售单 ID |

成功响应 `data` 字段：同 `4.3 list[]` 单项字段

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 销售单不存在 | 404 | `sale-out-not-found` |

### 4.5 删除销售出库单

- URL：`DELETE /api/v1/sale-outs/{id}`
- 权限：`sale:order:delete`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 销售单 ID |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 销售单不存在 | 404 | `sale-out-not-found` |
| 库存回滚冲突 | 400 | `inventory-rollback-conflict` |

## 5. 库存接口

### 5.1 分页查询库存汇总

- URL：`GET /api/v1/inventories`
- 权限：`inventory:stock:list`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| pageNum | Long | 否 | 页码 |
| pageSize | Long | 否 | 每页条数 |
| warehouseId | Long | 否 | 仓库 ID |
| productId | Long | 否 | 商品 ID |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| list | Array<Object> | 是 | 库存汇总列表 |
| list[].id | Long | 是 | 库存记录 ID |
| list[].warehouseId | Long | 是 | 仓库 ID |
| list[].productId | Long | 是 | 商品 ID |
| list[].totalQty | BigDecimal | 是 | 总数量 |
| list[].lockedQty | BigDecimal | 是 | 锁定数量 |
| list[].availableQty | BigDecimal | 是 | 可用数量 |
| total | Long | 是 | 总记录数 |

### 5.2 查询库存批次明细

- URL：`GET /api/v1/inventories/{productId}/batches`
- 权限：`inventory:batch:list`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| productId | Long | 是 | 商品 ID |

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 否 | 仓库 ID |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| [] | Array<Object> | 是 | 批次数组 |
| [][].id | Long | 是 | 批次 ID |
| [][].warehouseId | Long | 是 | 仓库 ID |
| [][].productId | Long | 是 | 商品 ID |
| [][].batchNo | String | 是 | 批次号 |
| [][].inboundTime | String(LocalDateTime) | 是 | 入库时间 |
| [][].productionDate | String(LocalDate) | 否 | 生产日期 |
| [][].expiryDate | String(LocalDate) | 否 | 过期日期 |
| [][].unitPrice | BigDecimal | 是 | 单价 |
| [][].initQty | BigDecimal | 是 | 初始数量 |
| [][].remainQty | BigDecimal | 是 | 剩余数量 |

### 5.3 查询库存流水

- URL：`GET /api/v1/inventories/flows`
- 权限：`inventory:flow:list`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 否 | 仓库 ID |
| productId | Long | 否 | 商品 ID |
| bizTimeFrom | String(LocalDateTime) | 否 | 开始时间 |
| bizTimeTo | String(LocalDateTime) | 否 | 结束时间 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| [] | Array<Object> | 是 | 流水数组 |
| [][].id | Long | 是 | 流水 ID |
| [][].warehouseId | Long | 是 | 仓库 ID |
| [][].productId | Long | 是 | 商品 ID |
| [][].batchId | Long | 否 | 批次 ID |
| [][].flowType | String | 是 | 流水类型 |
| [][].bizType | String | 是 | 业务类型 |
| [][].bizId | Long | 是 | 单据 ID |
| [][].bizItemId | Long | 否 | 单据明细 ID |
| [][].changeQty | BigDecimal | 是 | 变动数量 |
| [][].afterQty | BigDecimal | 是 | 变动后数量 |
| [][].bizTime | String(LocalDateTime) | 是 | 业务时间 |
| [][].remark | String | 否 | 备注 |

### 5.4 修改库存汇总

- URL：`PUT /api/v1/inventories/{id}`
- 权限：`inventory:stock:update`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 库存记录 ID |

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| totalQty | BigDecimal | 是 | 总库存数量 |
| availableQty | BigDecimal | 是 | 可用库存数量 |
| remark | String | 否 | 修正备注 |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 总数量为空 | 400 | `total-qty-required` |
| 可用数量为空 | 400 | `available-qty-required` |
| 库存不存在 | 404 | `inventory-not-found` |

### 5.5 删除库存记录

- URL：`DELETE /api/v1/inventories/{id}`
- 权限：`inventory:stock:delete`

路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 库存记录 ID |

成功响应：`data = null`

错误响应：

| 场景 | code | message |
| --- | --- | --- |
| 库存不存在 | 404 | `inventory-not-found` |

## 6. 统计接口

### 6.1 按日查询进出货统计

- URL：`GET /api/v1/statistics/daily/inbound-outbound`
- 权限：`statistics:daily:view`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 否 | 仓库 ID |
| statType | String | 否 | 统计类型，如 `sale/purchase` |
| dateFrom | String(LocalDate) | 否 | 开始日期 |
| dateTo | String(LocalDate) | 否 | 结束日期 |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| [] | Array<Object> | 是 | 日统计数组 |
| [][].statType | String | 是 | 统计类型 |
| [][].statDate | String(LocalDate) | 是 | 统计日期 |
| [][].totalQty | BigDecimal | 是 | 总数量 |
| [][].totalAmount | BigDecimal | 是 | 总金额 |

### 6.2 按月查询进出货统计

- URL：`GET /api/v1/statistics/monthly/inbound-outbound`
- 权限：`statistics:monthly:view`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 否 | 仓库 ID |
| statType | String | 否 | 统计类型 |
| monthFrom | String | 否 | 开始月份，格式 `YYYY-MM` |
| monthTo | String | 否 | 结束月份，格式 `YYYY-MM` |

成功响应 `data` 字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| [] | Array<Object> | 是 | 月统计数组 |
| [][].key | String | 是 | 聚合维度键值 |
| [][].totalAmount | BigDecimal | 是 | 总金额 |
| [][].totalQty | BigDecimal | 是 | 总数量 |

### 6.3 按客户统计销售金额

- URL：`GET /api/v1/statistics/amount-by-customer`
- 权限：`statistics:customer:view`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 否 | 仓库 ID |
| customerId | Long | 否 | 客户 ID |
| dateFrom | String(LocalDate) | 否 | 开始日期 |
| dateTo | String(LocalDate) | 否 | 结束日期 |
| monthFrom | String | 否 | 开始月份 |
| monthTo | String | 否 | 结束月份 |

成功响应 `data` 字段：同 `6.2`

### 6.4 按供应商统计采购金额

- URL：`GET /api/v1/statistics/amount-by-supplier`
- 权限：`statistics:supplier:view`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 否 | 仓库 ID |
| supplierId | Long | 否 | 供应商 ID |
| dateFrom | String(LocalDate) | 否 | 开始日期 |
| dateTo | String(LocalDate) | 否 | 结束日期 |
| monthFrom | String | 否 | 开始月份 |
| monthTo | String | 否 | 结束月份 |

成功响应 `data` 字段：同 `6.2`

### 6.5 按分类统计金额

- URL：`GET /api/v1/statistics/amount-by-category`
- 权限：`statistics:category:view`

查询参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| warehouseId | Long | 否 | 仓库 ID |
| statType | String | 否 | 统计类型 |
| dateFrom | String(LocalDate) | 否 | 开始日期 |
| dateTo | String(LocalDate) | 否 | 结束日期 |
| monthFrom | String | 否 | 开始月份 |
| monthTo | String | 否 | 结束月份 |

成功响应 `data` 字段：同 `6.2`

## 对齐说明

- 当前 API 文档只覆盖代码中已存在的 Controller、DTO、VO。
- 当前项目未发现独立“记账”控制器或账务实体表，因此本文档不包含独立总账、分录、应收应付类接口。
- 如需继续增强，可下一步再补充“每个接口完整 JSON 示例”版本。
