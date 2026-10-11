# 当前项目 PRD 与接口文档

## 文档信息

- 项目：米面粮油店进销存与记账管理系统
- 生成日期：2026-04-17
- 代码对齐范围：`src/main/java/org/liangyou/modules`
- 对齐方式：严格对齐当前已有 Controller、DTO、Entity、VO、统一返回结构
- 文档范围：PRD、API、数据模型、错误码、权限说明、一致性检查、质量评审、修复说明

## 第5阶段：Code Context Skill 结果

### 已识别模块

- 认证：`auth`
- 用户与权限：`user`
- 商品基础资料：`product`
- 库存：`inventory`
- 采购入库：`purchase`
- 销售出库：`sale`
- 统计报表：`statistics`

### 当前代码已有接口

- 新增：无，本文档不发明未落地接口
- 复用：
  - `/api/v1/auth/*`
  - `/api/v1/users/*`
  - `/api/v1/roles/*`
  - `/api/v1/menus/tree`
  - `/api/v1/purchase-ins/*`
  - `/api/v1/sale-outs/*`
  - `/api/v1/inventories/*`
  - `/api/v1/statistics/*`
- 修改：无，本文档仅对当前实现做梳理与归档

### 代码对齐结论

- 当前项目已经实现进货、出货、库存、统计、认证、RBAC 权限。
- 当前项目未发现独立“记账”控制器或账务实体表。
- 因此本文档将“记账能力”定义为：
  - 已实现部分：采购单总金额、销售单总金额、统计报表、库存流水
  - 未独立实现部分：独立总账、收支分录、应收应付台账

## 最终 PRD

### 1. 项目背景

当前项目面向米面粮油门店内部管理，目标是解决货物进出、库存追踪、金额记录、人员权限和经营统计分散的问题。系统定位为中小型门店的内部管理系统，优先保障业务闭环、数据可追溯和操作简单。

### 2. 产品目标

- 支持采购入库、销售出库、库存汇总、库存流水、批次追踪的完整业务闭环。
- 支持管理员和记账员基于 JWT + RBAC 的权限化操作。
- 支持按客户、供应商、分类、日/月维度统计数量和金额。
- 支持通过采购总额、销售总额与统计报表实现基础“记账/对账”能力。

### 3. 用户角色

- 管理员
  - 管理用户、角色、权限、菜单
  - 查看全部采购、销售、库存、统计数据
  - 调整库存、删除业务单据
- 记账员
  - 新增和查询采购入库单
  - 新增和查询销售出库单
  - 查询库存、库存流水、统计报表
  - 不负责角色权限配置

### 4. 功能模块

- `P0` 认证登录
  - 用户登录、退出、获取当前登录人
- `P0` 用户与权限
  - 用户管理、角色管理、菜单树、用户授权、角色授权
- `P0` 采购入库
  - 创建、修改、分页查询、详情、删除采购入库单
- `P0` 销售出库
  - 创建、修改、分页查询、详情、删除销售出库单
- `P0` 库存管理
  - 库存汇总查询、批次明细、库存流水、库存修正、库存删除
- `P1` 统计报表
  - 日/月进出货、按客户、按供应商、按分类统计
- `P1` 商品基础数据
  - 代码中已有 `base_product` 实体，但当前未暴露独立商品 Controller
- `P1` 记账能力
  - 当前通过采购单 `totalAmount`、销售单 `totalAmount`、统计模块承载
  - 独立台账模块暂未实现，不纳入现有 API 范围

### 5. 业务流程

#### 主流程 A：采购入库

1. 记账员创建采购入库单，填写仓库、供应商、业务日期、明细。
2. 系统写入 `biz_purchase_in` 与 `biz_purchase_in_item`。
3. 系统新增或更新库存汇总 `inv_stock`。
4. 系统新增库存批次 `inv_batch`。
5. 系统写入库存流水 `inv_stock_flow`。
6. 系统通过 `totalAmount` 形成采购支出记录基础数据。

#### 主流程 B：销售出库

1. 记账员创建销售出库单，填写仓库、客户、业务日期、明细。
2. 系统写入 `biz_sale_out` 与 `biz_sale_out_item`。
3. 系统按 FIFO 扣减库存批次。
4. 系统更新库存汇总 `inv_stock`。
5. 系统写入库存流水 `inv_stock_flow`。
6. 系统通过 `totalAmount` 形成销售收入记录基础数据。

#### 补充流程 C：库存调整

1. 管理员直接修改库存汇总数量。
2. 系统更新 `inv_stock`。
3. 系统写入调整型库存流水。

#### 异常流程

- 销售数量大于可用库存
  - 系统拒绝出库，返回 `400 insufficient-stock`
- 修改或删除已被后续业务消耗的采购单
  - 系统拒绝回滚，返回 `400 purchase-in-already-consumed`
- 回滚库存出现数量冲突
  - 系统拒绝操作，返回 `400 inventory-rollback-conflict`
- JWT 无效或未登录
  - 系统拒绝访问，返回 `401 未登录或 Token 无效`

### 6. 数据模型（字段级）

#### 商品 `base_product`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 主键 |
| productCode | String | 是 | 商品编码 |
| productName | String | 是 | 商品名称 |
| categoryId | Long | 否 | 分类 ID |
| brandName | String | 否 | 品牌名 |
| spec | String | 否 | 规格 |
| barcode | String | 否 | 条码 |
| imageUrl | String | 否 | 图片 |
| baseUnitId | Long | 否 | 基础单位 |
| shelfLifeDays | Integer | 否 | 保质期天数 |
| status | Integer | 是 | 状态 |
| remark | String | 否 | 备注 |

#### 采购入库单 `biz_purchase_in`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 主键 |
| orderNo | String | 是 | 入库单号 |
| warehouseId | Long | 是 | 仓库 ID |
| supplierId | Long | 是 | 供应商 ID |
| bizDate | LocalDate | 是 | 业务日期 |
| totalAmount | BigDecimal | 是 | 总金额 |
| status | Integer | 是 | 状态 |
| remark | String | 否 | 备注 |

#### 采购入库明细 `biz_purchase_in_item`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 主键 |
| purchaseInId | Long | 是 | 入库单 ID |
| warehouseId | Long | 是 | 仓库 ID |
| productId | Long | 是 | 商品 ID |
| unitId | Long | 是 | 单位 ID |
| qty | BigDecimal | 是 | 数量 |
| unitPrice | BigDecimal | 是 | 单价 |
| amount | BigDecimal | 是 | 金额 |
| batchNo | String | 是 | 批次号 |
| productionDate | LocalDate | 否 | 生产日期 |
| expiryDate | LocalDate | 否 | 过期日期 |
| inboundTime | LocalDateTime | 是 | 入库时间 |
| remark | String | 否 | 备注 |

#### 销售出库单 `biz_sale_out`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 主键 |
| orderNo | String | 是 | 出库单号 |
| warehouseId | Long | 是 | 仓库 ID |
| customerId | Long | 是 | 客户 ID |
| bizDate | LocalDate | 是 | 业务日期 |
| totalAmount | BigDecimal | 是 | 总金额 |
| status | Integer | 是 | 状态 |
| remark | String | 否 | 备注 |

#### 销售出库明细 `biz_sale_out_item`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 主键 |
| saleOutId | Long | 是 | 出库单 ID |
| warehouseId | Long | 是 | 仓库 ID |
| productId | Long | 是 | 商品 ID |
| unitId | Long | 是 | 单位 ID |
| qty | BigDecimal | 是 | 数量 |
| unitPrice | BigDecimal | 是 | 销售单价 |
| amount | BigDecimal | 是 | 金额 |
| remark | String | 否 | 备注 |

#### 库存汇总 `inv_stock`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 主键 |
| warehouseId | Long | 是 | 仓库 ID |
| productId | Long | 是 | 商品 ID |
| totalQty | BigDecimal | 是 | 总数量 |
| lockedQty | BigDecimal | 是 | 锁定数量 |
| availableQty | BigDecimal | 是 | 可用数量 |

#### 库存批次 `inv_batch`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 主键 |
| warehouseId | Long | 是 | 仓库 ID |
| productId | Long | 是 | 商品 ID |
| sourceType | String | 是 | 来源类型 |
| sourceId | Long | 是 | 来源单据 ID |
| batchNo | String | 是 | 批次号 |
| inboundTime | LocalDateTime | 是 | 入库时间 |
| productionDate | LocalDate | 否 | 生产日期 |
| expiryDate | LocalDate | 否 | 过期日期 |
| unitPrice | BigDecimal | 是 | 单价 |
| initQty | BigDecimal | 是 | 初始数量 |
| remainQty | BigDecimal | 是 | 剩余数量 |
| status | Integer | 是 | 状态 |

#### 库存流水 `inv_stock_flow`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 主键 |
| warehouseId | Long | 是 | 仓库 ID |
| productId | Long | 是 | 商品 ID |
| batchId | Long | 否 | 批次 ID |
| flowType | String | 是 | 流水类型 |
| bizType | String | 是 | 业务类型 |
| bizId | Long | 是 | 单据 ID |
| bizItemId | Long | 否 | 单据明细 ID |
| changeQty | BigDecimal | 是 | 变动数量 |
| afterQty | BigDecimal | 是 | 变动后数量 |
| bizTime | LocalDateTime | 是 | 业务时间 |
| remark | String | 否 | 备注 |

### 7. 非功能需求

- 鉴权：JWT
- 权限模型：RBAC，管理员全量，记账员仅业务操作
- 接口风格：REST
- 统一返回结构：`{ code, message, data }`
- 分页参数：`pageNum/pageSize`
- 默认分页：`1/10`
- 最大页大小：建议文档约束为 `100`
- 性能目标：P95 小于 `500ms`
- 并发目标：20-50 并发
- 审计要求：登录、用户权限变更、采购入库、销售出库、库存调整必须可追溯

## 最终 API 文档

### 通用规范

- Base URL：`/api/v1`
- 认证头：`Authorization: Bearer {token}`
- 成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

- 分页响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 0
  }
}
```

### 错误码

| 错误码 | 含义 | 来源 |
| --- | --- | --- |
| 400 | 参数校验失败/业务错误 | `GlobalExceptionHandler` |
| 401 | 未登录或 Token 无效 | `AuthService` / `SecurityConfig` |
| 404 | 资源不存在 | 业务 Service |

### 业务错误消息

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
- `未登录或 Token 无效`

### 认证接口

#### 1. 用户登录

- 接口名称：用户登录
- URL：`POST /api/v1/auth/login`
- 权限控制：匿名可访问
- 请求参数：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| username | String | 是 |
| password | String | 是 |

- 返回结构（JSON）：

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
      "permissions": ["purchase:order:list"]
    }
  }
}
```

- 错误码：`400` 用户名或密码错误

#### 2. 获取当前登录用户

- 接口名称：获取当前登录用户信息
- URL：`GET /api/v1/auth/me`
- 权限控制：已登录
- 请求参数：无
- 返回结构：`CurrentUserResponse`
- 错误码：`401`

#### 3. 退出登录

- 接口名称：退出登录
- URL：`POST /api/v1/auth/logout`
- 权限控制：已登录
- 请求参数：请求头 `Authorization`
- 返回结构：`data = null`
- 错误码：`401`

### 用户与权限接口

#### 4. 新增用户

- URL：`POST /api/v1/users`
- 方法：`POST`
- 权限控制：`system:user:create`
- 请求参数：`username,password,realName,phone,email,warehouseId,status,isSuperAdmin`
- 返回结构：`data = 用户ID`
- 错误码：`400 username-exists`、参数校验错误

#### 5. 修改用户

- URL：`PUT /api/v1/users/{id}`
- 方法：`PUT`
- 权限控制：`system:user:update`
- 请求参数：`realName,phone,email,warehouseId,status,isSuperAdmin`
- 返回结构：`data = null`
- 错误码：`404 user-not-found`

#### 6. 删除用户

- URL：`DELETE /api/v1/users/{id}`
- 方法：`DELETE`
- 权限控制：`system:user:delete`
- 请求参数：路径参数 `id`
- 返回结构：`data = null`
- 错误码：`404 user-not-found`

#### 7. 查询用户详情

- URL：`GET /api/v1/users/{id}`
- 方法：`GET`
- 权限控制：`system:user:view`
- 请求参数：路径参数 `id`
- 返回结构：`UserDetailResponse{id,username,realName,phone,email,warehouseId,status,isSuperAdmin,roleIds}`
- 错误码：`404 user-not-found`

#### 8. 分页查询用户

- URL：`GET /api/v1/users`
- 方法：`GET`
- 权限控制：`system:user:list`
- 请求参数：`pageNum,pageSize,username,realName,warehouseId,status`
- 返回结构：`PageResponse<UserDetailResponse>`
- 错误码：无特殊业务错误

#### 9. 给用户分配角色

- URL：`POST /api/v1/users/{id}/roles`
- 方法：`POST`
- 权限控制：`system:user:grant-role`
- 请求参数：

```json
{
  "roleIds": [1, 2]
}
```

- 返回结构：`data = null`
- 错误码：`404 user-not-found`、`404 role-not-found`、`400 role-disabled`

#### 10. 给用户单独授权

- URL：`POST /api/v1/users/{id}/permissions`
- 方法：`POST`
- 权限控制：`system:user:grant-permission`
- 请求参数：

```json
{
  "grantPermissionIds": [201],
  "revokePermissionIds": [202]
}
```

- 返回结构：`data = null`
- 错误码：`400 permission-conflict`、`404 permission-not-found`、`400 permission-disabled`

#### 11. 查询用户最终权限

- URL：`GET /api/v1/users/{id}/permissions`
- 方法：`GET`
- 权限控制：`system:user:view-permission`
- 请求参数：路径参数 `id`
- 返回结构：`UserPermissionResponse{userId,roleIds,permissionIds}`
- 错误码：`404 user-not-found`

#### 12. 新增角色

- URL：`POST /api/v1/roles`
- 方法：`POST`
- 权限控制：`system:role:create`
- 请求参数：`roleCode,roleName,status,remark`
- 返回结构：`RoleResponse{id,roleCode,roleName,status,remark}`
- 错误码：`400 role-code-exists`

#### 13. 修改角色

- URL：`PUT /api/v1/roles/{id}`
- 方法：`PUT`
- 权限控制：`system:role:update`
- 请求参数：`roleCode,roleName,status,remark`
- 返回结构：`RoleResponse`
- 错误码：`404 role-not-found`

#### 14. 删除角色

- URL：`DELETE /api/v1/roles/{id}`
- 方法：`DELETE`
- 权限控制：`system:role:delete`
- 请求参数：路径参数 `id`
- 返回结构：`data = null`
- 错误码：`404 role-not-found`

#### 15. 查询角色列表

- URL：`GET /api/v1/roles`
- 方法：`GET`
- 权限控制：`system:role:list`
- 请求参数：`roleName,roleCode,status`
- 返回结构：`List<RoleResponse>`
- 错误码：无特殊业务错误

#### 16. 给角色分配权限

- URL：`POST /api/v1/roles/{id}/permissions`
- 方法：`POST`
- 权限控制：`system:role:grant`
- 请求参数：

```json
{
  "permissionIds": [101, 102]
}
```

- 返回结构：`data = null`
- 错误码：`404 role-not-found`、`404 permission-not-found`、`400 permission-disabled`

#### 17. 查询菜单树

- URL：`GET /api/v1/menus/tree`
- 方法：`GET`
- 权限控制：`system:menu:view`
- 请求参数：无
- 返回结构：`List<MenuTreeResponse{id,name,path,children}>`
- 错误码：`401`

### 采购入库接口

#### 18. 新增采购入库单

- URL：`POST /api/v1/purchase-ins`
- 方法：`POST`
- 权限控制：`purchase:order:create`
- 请求参数：

```json
{
  "warehouseId": 1,
  "supplierId": 10,
  "bizDate": "2026-04-16",
  "status": 1,
  "remark": "首批采购",
  "items": [
    {
      "productId": 1001,
      "unitId": 1,
      "qty": 100,
      "unitPrice": 120.5,
      "amount": 12050,
      "batchNo": "B20260416001",
      "productionDate": "2026-04-01",
      "expiryDate": "2026-10-01",
      "inboundTime": "2026-04-16T10:00:00",
      "remark": "整箱入库"
    }
  ]
}
```

- 返回结构：`data = 采购单ID`
- 错误码：参数校验错误

#### 19. 修改采购入库单

- URL：`PUT /api/v1/purchase-ins/{id}`
- 方法：`PUT`
- 权限控制：`purchase:order:update`
- 请求参数：同新增采购单
- 返回结构：`data = null`
- 错误码：`404 purchase-in-not-found`、`400 purchase-in-already-consumed`、`400 inventory-rollback-conflict`

#### 20. 分页查询采购入库单

- URL：`GET /api/v1/purchase-ins`
- 方法：`GET`
- 权限控制：`purchase:order:list`
- 请求参数：`pageNum,pageSize,orderNo,warehouseId,supplierId,status,bizDateFrom,bizDateTo`
- 返回结构：`PageResponse<PurchaseInDetailResponse>`
- 错误码：无特殊业务错误

#### 21. 查询采购入库单详情

- URL：`GET /api/v1/purchase-ins/{id}`
- 方法：`GET`
- 权限控制：`purchase:order:view`
- 请求参数：路径参数 `id`
- 返回结构：`PurchaseInDetailResponse{id,orderNo,warehouseId,supplierId,bizDate,totalAmount,status,remark,items[]}`
- 错误码：`404 purchase-in-not-found`

#### 22. 删除采购入库单

- URL：`DELETE /api/v1/purchase-ins/{id}`
- 方法：`DELETE`
- 权限控制：`purchase:order:delete`
- 请求参数：路径参数 `id`
- 返回结构：`data = null`
- 错误码：`404 purchase-in-not-found`、`400 purchase-in-already-consumed`、`400 inventory-rollback-conflict`

### 销售出库接口

#### 23. 新增销售出库单

- URL：`POST /api/v1/sale-outs`
- 方法：`POST`
- 权限控制：`sale:order:create`
- 请求参数：

```json
{
  "warehouseId": 1,
  "customerId": 20,
  "bizDate": "2026-04-16",
  "status": 1,
  "remark": "日常销售",
  "items": [
    {
      "productId": 1001,
      "unitId": 1,
      "qty": 20,
      "unitPrice": 150,
      "amount": 3000,
      "remark": "门店零售"
    }
  ]
}
```

- 返回结构：`data = 销售单ID`
- 错误码：`400 insufficient-stock`

#### 24. 修改销售出库单

- URL：`PUT /api/v1/sale-outs/{id}`
- 方法：`PUT`
- 权限控制：`sale:order:update`
- 请求参数：同新增销售单
- 返回结构：`data = null`
- 错误码：`404 sale-out-not-found`、`400 insufficient-stock`、`400 inventory-rollback-conflict`

#### 25. 分页查询销售出库单

- URL：`GET /api/v1/sale-outs`
- 方法：`GET`
- 权限控制：`sale:order:list`
- 请求参数：`pageNum,pageSize,orderNo,warehouseId,customerId,status,bizDateFrom,bizDateTo`
- 返回结构：`PageResponse<SaleOutDetailResponse>`
- 错误码：无特殊业务错误

#### 26. 查询销售出库单详情

- URL：`GET /api/v1/sale-outs/{id}`
- 方法：`GET`
- 权限控制：`sale:order:view`
- 请求参数：路径参数 `id`
- 返回结构：`SaleOutDetailResponse{id,orderNo,warehouseId,customerId,bizDate,totalAmount,status,remark,items[]}`
- 错误码：`404 sale-out-not-found`

#### 27. 删除销售出库单

- URL：`DELETE /api/v1/sale-outs/{id}`
- 方法：`DELETE`
- 权限控制：`sale:order:delete`
- 请求参数：路径参数 `id`
- 返回结构：`data = null`
- 错误码：`404 sale-out-not-found`、`400 inventory-rollback-conflict`

### 库存接口

#### 28. 分页查询库存汇总

- URL：`GET /api/v1/inventories`
- 方法：`GET`
- 权限控制：`inventory:stock:list`
- 请求参数：`pageNum,pageSize,warehouseId,productId`
- 返回结构：`PageResponse<InventoryResponse{id,warehouseId,productId,totalQty,lockedQty,availableQty}>`
- 错误码：无特殊业务错误

#### 29. 查询库存批次明细

- URL：`GET /api/v1/inventories/{productId}/batches`
- 方法：`GET`
- 权限控制：`inventory:batch:list`
- 请求参数：路径参数 `productId`，查询参数 `warehouseId`
- 返回结构：`List<InventoryBatchResponse>`
- 错误码：无特殊业务错误

#### 30. 查询库存流水

- URL：`GET /api/v1/inventories/flows`
- 方法：`GET`
- 权限控制：`inventory:flow:list`
- 请求参数：`warehouseId,productId,bizTimeFrom,bizTimeTo`
- 返回结构：`List<InventoryFlowResponse>`
- 错误码：无特殊业务错误

#### 31. 修改库存汇总

- URL：`PUT /api/v1/inventories/{id}`
- 方法：`PUT`
- 权限控制：`inventory:stock:update`
- 请求参数：当前代码校验字段至少包括 `totalQty,availableQty`
- 返回结构：`data = null`
- 错误码：`404 inventory-not-found`

#### 32. 删除库存记录

- URL：`DELETE /api/v1/inventories/{id}`
- 方法：`DELETE`
- 权限控制：`inventory:stock:delete`
- 请求参数：路径参数 `id`
- 返回结构：`data = null`
- 错误码：`404 inventory-not-found`

### 统计接口

#### 33. 按日查询进出货统计

- URL：`GET /api/v1/statistics/daily/inbound-outbound`
- 方法：`GET`
- 权限控制：`statistics:daily:view`
- 请求参数：`warehouseId,statType,dateFrom,dateTo`
- 返回结构：`List<DailyInboundOutboundResponse{statType,statDate,totalQty,totalAmount}>`
- 错误码：无特殊业务错误

#### 34. 按月查询进出货统计

- URL：`GET /api/v1/statistics/monthly/inbound-outbound`
- 方法：`GET`
- 权限控制：`statistics:monthly:view`
- 请求参数：`warehouseId,statType,monthFrom,monthTo`
- 返回结构：`List<AmountSummaryResponse{key,totalAmount,totalQty}>`
- 错误码：无特殊业务错误

#### 35. 按客户统计销售金额

- URL：`GET /api/v1/statistics/amount-by-customer`
- 方法：`GET`
- 权限控制：`statistics:customer:view`
- 请求参数：`warehouseId,customerId,dateFrom,dateTo,monthFrom,monthTo`
- 返回结构：`List<AmountSummaryResponse>`
- 错误码：无特殊业务错误

#### 36. 按供应商统计采购金额

- URL：`GET /api/v1/statistics/amount-by-supplier`
- 方法：`GET`
- 权限控制：`statistics:supplier:view`
- 请求参数：`warehouseId,supplierId,dateFrom,dateTo,monthFrom,monthTo`
- 返回结构：`List<AmountSummaryResponse>`
- 错误码：无特殊业务错误

#### 37. 按分类统计金额

- URL：`GET /api/v1/statistics/amount-by-category`
- 方法：`GET`
- 权限控制：`statistics:category:view`
- 请求参数：`warehouseId,statType,dateFrom,dateTo,monthFrom,monthTo`
- 返回结构：`List<AmountSummaryResponse>`
- 错误码：无特殊业务错误

## 一致性检查结果

### 检查项

- PRD 与 API 是否一致：通过
- API 与代码上下文是否冲突：基本通过
- 字段命名是否统一：通过
- 是否缺失模块接口：存在边界说明，已修复

### 结论

- PRD 中的采购、销售、库存、统计、认证、权限均与当前代码一致。
- 分页风格、JWT、统一返回结构均与当前代码一致。
- “记账”在用户需求中存在，但当前代码未提供独立账务模块；本文档已修正为“基础记账能力已体现在金额与统计中，独立账务模块不纳入当前 API”。

## 问题清单

| 问题描述 | 类型 | 严重级别 | 修复建议 |
| --- | --- | --- | --- |
| 用户提出“记账功能”，但代码中没有独立账务 Controller/Entity | 隐性问题 | 高 | 不虚构接口，明确记账边界，仅按现有金额与统计能力落文档 |
| 商品基础资料有实体但未见商品 Controller，PRD 若写成已支持商品管理会与代码冲突 | 隐性问题 | 高 | 在 PRD 中标记为已有实体、未暴露独立 API |
| 库存修改接口请求字段未在控制器处直观展示完整业务含义 | 规范问题 | 中 | 文档仅写当前已知强校验字段，并建议补充 DTO 注释或独立接口文档 |
| 业务错误消息同时存在中文和英文短码，错误码规范不完全统一 | 规范问题 | 中 | 建议后续统一为错误码短语或统一国际化策略 |
| 统计接口返回的 `key` 为通用聚合键，语义不够显式 | 规范问题 | 低 | 后续可拆分为 `supplierId/customerId/categoryId + name` 等更清晰字段 |

## 修复说明

- 修复了“记账能力”定义，将其限定为当前已实现的采购金额、销售金额、统计报表与库存流水，不生成不存在的账务 API。
- 修复了“商品管理”表述，将其从“现有可用接口模块”调整为“现有实体、未发现独立 Controller”。
- 修复了 API 文档中的风格定义，统一采用当前代码中的 `/api/v1/*` 路由、`ApiResponse` 包装、`pageNum/pageSize` 分页。
- 修复了权限说明，全部对齐控制器注解里的权限点字符串。

## 设计亮点

- 与当前代码严格对齐，没有虚构控制器和数据表。
- 采购、销售、库存、统计形成闭环，足以支撑门店级进销存主流程。
- JWT + RBAC 已成体系，适合继续扩展前端管理台。
- 文档已提前暴露“独立账务模块缺失”这一后续演进点，便于下一阶段规划。
