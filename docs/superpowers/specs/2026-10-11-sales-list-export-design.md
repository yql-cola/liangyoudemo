# 销售清单记录与导出功能设计

> 历史设计稿：本文中的“保存即扣库存”已被 [统一销售开单与配送需求设计](2026-10-19-unified-sales-delivery-design.md) 取代。新需求以该文件为准；本文件仅保留早期纸质清单字段和导出思路供参考。

## 1. 背景与结论

用户提供的纸质单据是“粮油配送中心销售清单”，核心内容包括客户、录单时间、单据编号、商品明细、经手人、电话、地址、本单应收金额。用户已确认：

- 销售清单保存后必须扣减库存。
- 导出需要同时支持 PDF 和 Excel。
- 客户信息从客户档案带出，但允许在本单修改，并保存为历史单据快照。

当前系统已经存在销售出库模块，后端包含 `biz_sale_out`、`biz_sale_out_item`、FIFO 库存扣减、销售单查询、详情、修改、删除；前端已有销售出库列表、录入弹窗、详情页。因此本功能不新增一套独立销售业务，而是在现有销售出库上扩展“销售清单记录 + PDF/Excel 导出”能力。

## 2. 当前系统满足度评估

| 能力 | 当前状态 | 是否满足 | 处理方式 |
| --- | --- | --- | --- |
| 销售单主表 | 已有 `biz_sale_out` | 部分满足 | 复用，并补充客户快照、经手人、联系方式、地址、录单时间 |
| 销售明细 | 已有 `biz_sale_out_item` | 部分满足 | 复用，并在响应中补充商品名称、单位名称 |
| 库存扣减 | 已有 FIFO 扣减和库存流水 | 满足 | 继续沿用保存即扣库存 |
| 客户档案 | 已有 `base_customer` 表 | 部分满足 | 当前缺少客户基础资料接口，需要补齐查询/详情能力 |
| 商品/单位名称展示 | 表结构已有商品和单位 | 部分满足 | 当前销售响应主要返回 ID，需要后端组装名称字段 |
| 销售清单导出 | 未实现 | 不满足 | 新增 PDF、Excel 导出接口 |
| 前端打印/导出入口 | 未实现 | 不满足 | 销售详情和列表增加导出按钮 |

## 3. 设计目标

1. 用户在销售管理中新增或编辑销售清单，保存后按现有规则扣减库存。
2. 销售清单详情展示接近纸质单据的信息，而不是仅显示 ID。
3. 导出 PDF 用于打印或留档，导出 Excel 用于对账和二次整理。
4. 客户名称、联系人、电话、地址按单据快照保存，历史销售清单不受客户档案后续变更影响。
5. 保持当前接口风格、权限点、返回结构和前端技术栈一致。

## 4. 业务规则

### 4.1 销售清单生命周期

- 新增：选择仓库、客户、业务日期，系统带出客户档案信息，用户可修改经手人、电话、地址；填写商品明细后保存。
- 保存：创建销售出库单，计算明细金额和整单金额，按 FIFO 扣减库存，记录库存批次扣减和库存流水。
- 修改：沿用当前销售出库更新策略，回滚旧单库存影响，再按新单内容重新扣减。
- 删除：沿用当前逻辑删除和库存回滚策略。
- 导出：仅对已保存销售单导出，导出内容读取单据快照和明细名称。

### 4.2 金额计算

- 明细金额：`qty * unitPrice`，后端重新计算并以服务端结果为准。
- 本单应收：所有明细金额求和，对应 `totalAmount`。
- 前端可实时预览金额，但不能作为最终可信来源。

### 4.3 客户快照

创建销售清单时：

- 用户选择 `customerId`。
- 后端读取 `base_customer` 的 `customer_name`、`contact_name`、`contact_phone`、`address`。
- 请求中如果传入经手人、电话、地址，则使用请求值覆盖默认带出值。
- 最终写入销售单主表快照字段。

导出和历史详情统一读取销售单快照，不再实时读取客户档案覆盖。

## 5. 数据库设计

### 5.1 修改 `biz_sale_out`

建议新增字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `customer_name_snapshot` | `VARCHAR(128)` | 是 | 客户名称快照 |
| `handler_name` | `VARCHAR(64)` | 否 | 经手人 |
| `contact_phone_snapshot` | `VARCHAR(32)` | 否 | 联系电话快照 |
| `delivery_address_snapshot` | `VARCHAR(255)` | 否 | 地址快照 |
| `recorded_time` | `DATETIME` | 是 | 录单时间，默认当前时间 |

说明：

- `order_no` 继续作为单据编号。可保持当前 `SOyyyyMMddHHmmssSSS`，也可改为业务更友好的 `PXX-yyyyMMdd-序号`。本期建议保持现有格式，避免引入并发序号生成复杂度。
- `biz_date` 表示业务日期；`recorded_time` 表示录入时间，用于匹配纸质单据“录单时间”。

### 5.2 明细表

`biz_sale_out_item` 暂不新增字段。商品名称、单位名称通过查询 `base_product` 和 `base_unit` 组装返回和导出。

如后续要求历史单据在商品改名后仍完全不变，可再追加 `product_name_snapshot`、`unit_name_snapshot`。本期先不增加，降低表结构扩展范围。

## 6. 后端接口设计

### 6.1 新增/修改销售清单

复用现有接口：

- `POST /api/v1/sale-outs`
- `PUT /api/v1/sale-outs/{id}`

请求体在现有字段基础上扩展：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `handlerName` | String | 否 | 经手人 |
| `contactPhone` | String | 否 | 本单联系电话 |
| `deliveryAddress` | String | 否 | 本单地址 |
| `items[].amount` | BigDecimal | 否 | 可由前端传入，但后端重新计算 |

后端处理：

1. 校验仓库、客户、明细、数量、单价。
2. 查询客户档案，生成客户快照。
3. 计算明细金额和总金额。
4. 插入销售单和明细。
5. 按 FIFO 扣减库存。

### 6.2 销售清单详情

复用并扩展：

- `GET /api/v1/sale-outs/{id}`

响应新增字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `customerName` | String | 客户名称快照 |
| `handlerName` | String | 经手人 |
| `contactPhone` | String | 联系电话快照 |
| `deliveryAddress` | String | 地址快照 |
| `recordedTime` | String(LocalDateTime) | 录单时间 |
| `items[].productName` | String | 商品名称 |
| `items[].unitName` | String | 单位名称 |

### 6.3 分页查询销售清单

复用并扩展：

- `GET /api/v1/sale-outs`

查询参数新增：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `customerName` | String | 按客户名称快照模糊查询 |
| `recordedTimeFrom` | String(LocalDateTime) | 录单开始时间 |
| `recordedTimeTo` | String(LocalDateTime) | 录单结束时间 |

列表响应显示 `customerName`、`recordedTime`，减少前端只显示 ID 的问题。

### 6.4 导出 PDF

新增接口：

- `GET /api/v1/sale-outs/{id}/export/pdf`
- 权限：`sale:order:export`
- 响应：`application/pdf`
- 文件名：`销售清单-{orderNo}.pdf`

PDF 内容：

- 标题：粮油配送中心销售清单
- 客户：`customerName`
- 录单时间：`recordedTime`
- 单据编号：`orderNo`
- 明细表：商品名称、单位、数量、单价、金额、明细备注
- 页脚：经手人、电话、地址、本单应收

实现建议：

- 后端生成 PDF，前端直接下载。
- PDF 库建议使用 OpenPDF 或 iText 兼容方案；需确认中文字体加载，避免中文乱码。

### 6.5 导出 Excel

新增接口：

- `GET /api/v1/sale-outs/{id}/export/excel`
- 权限：`sale:order:export`
- 响应：`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- 文件名：`销售清单-{orderNo}.xlsx`

Excel 内容：

- Sheet 名称：`销售清单`
- 顶部信息：客户、录单时间、单据编号、经手人、电话、地址
- 明细列：商品名称、单位、数量、单价、金额、明细备注
- 底部合计：本单应收

实现建议：

- 后端生成 Excel，前端直接下载。
- 可引入 Apache POI，当前 `pom.xml` 尚未包含 Excel/PDF 依赖。

### 6.6 客户档案查询

当前前端基础资料为占位页，销售录入只能输入客户 ID。为支撑“选择客户并带出信息”，建议新增基础客户接口：

- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`

最小返回字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 客户 ID |
| `customerCode` | String | 客户编码 |
| `customerName` | String | 客户名称 |
| `contactName` | String | 联系人 |
| `contactPhone` | String | 联系电话 |
| `address` | String | 地址 |

## 7. 前端设计

### 7.1 销售列表页

现有页面：`frontend/src/views/sale/out/index.vue`

调整点：

- 标题和菜单可继续叫“销售出库”，页面内增强为销售清单录入；也可把菜单名称改为“销售清单”。
- 查询条件增加客户名称、录单时间范围。
- 表格列从“客户ID”优化为“客户名称”，保留单据编号、业务日期、录单时间、总金额。
- 操作列增加：
  - 详情
  - 编辑
  - 删除
  - 导出 PDF
  - 导出 Excel

### 7.2 新增/编辑弹窗

调整点：

- 客户选择从数字输入改为可搜索下拉。
- 选择客户后自动填充经手人、电话、地址。
- 经手人、电话、地址允许本单修改。
- 商品明细中商品和单位从 ID 输入优化为名称选择或显示。
- 明细金额前端自动计算，保存时仍由后端复算。

### 7.3 详情页

现有页面：`frontend/src/views/sale/out/detail.vue`

调整点：

- 顶部展示销售清单格式信息：客户、录单时间、单据编号。
- 明细表展示商品名称、单位、数量、单价、金额、备注。
- 底部展示经手人、手机、地址、本单应收。
- 顶部提供 PDF、Excel 导出按钮。

### 7.4 前端 API

现有文件：`frontend/src/api/sale/saleOut.js`

新增方法：

```js
export function exportSaleOutPdf(id) {
  return request({ url: `/v1/sale-outs/${id}/export/pdf`, method: 'get', responseType: 'blob' })
}

export function exportSaleOutExcel(id) {
  return request({ url: `/v1/sale-outs/${id}/export/excel`, method: 'get', responseType: 'blob' })
}
```

另新增客户 API 文件：

```js
export function listCustomer(params) {
  return request({ url: '/v1/customers', method: 'get', params })
}

export function getCustomer(id) {
  return request({ url: `/v1/customers/${id}`, method: 'get' })
}
```

## 8. 权限设计

沿用现有权限：

- `sale:order:list`
- `sale:order:view`
- `sale:order:create`
- `sale:order:update`
- `sale:order:delete`

新增：

- `sale:order:export`：允许导出 PDF 和 Excel。

客户选择需要新增：

- `basic:customer:list`
- `basic:customer:view`

## 9. 错误处理

| 场景 | HTTP code | message |
| --- | --- | --- |
| 销售单不存在 | 404 | `sale-out-not-found` |
| 客户不存在或停用 | 400 | `customer-not-found` |
| 商品不存在或停用 | 400 | `product-not-found` |
| 单位不存在 | 400 | `unit-not-found` |
| 数量小于等于 0 | 400 | `qty-must-positive` |
| 单价小于 0 | 400 | `unit-price-invalid` |
| 库存不足 | 400 | `insufficient-stock` |
| 导出失败 | 500 | `sale-export-failed` |

## 10. 测试设计

### 10.1 后端测试

- 创建销售清单时写入客户快照。
- 前端传入错误金额时，后端按数量和单价重新计算。
- 库存不足时不生成单据、不扣减库存。
- 修改销售清单时旧库存扣减被回滚，新库存扣减正确生成。
- 删除销售清单时库存回滚。
- 详情接口返回商品名称、单位名称、客户快照字段。
- PDF 导出响应头、文件名、中文内容可用。
- Excel 导出响应头、文件名、表格内容、合计金额正确。

### 10.2 前端测试

- 客户选择后自动填充经手人、电话、地址。
- 修改快照字段后保存，详情展示修改后的单据信息。
- 明细金额随数量和单价变化。
- PDF、Excel 按钮能下载文件。
- 无 `sale:order:export` 权限时隐藏导出按钮。

## 11. 实施顺序建议

1. 数据库补丁：销售单主表新增快照字段和录单时间。
2. 后端实体、DTO、VO 扩展。
3. 后端客户基础查询接口。
4. 销售详情查询补充名称字段。
5. PDF、Excel 导出接口。
6. 前端销售列表、表单、详情页调整。
7. 权限种子数据和接口文档更新。
8. 后端、前端测试补充。

## 12. 待确认项

当前设计已按以下默认值收敛：

- 单据编号本期沿用现有 `SOyyyyMMddHHmmssSSS` 格式，不改为图片中的 `PXX-yyyyMMdd-001`。
- 商品名称和单位名称本期查询实时主数据，不做明细快照。
- PDF 版式追求接近图片清单，但不要求完全复刻针式打印纸样式。

如后续必须严格复刻单号规则、打印纸尺寸或历史商品名称快照，需要在实施前追加字段和导出模板规则。
