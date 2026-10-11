# 基础资料与统计分析升级设计

## 1. 文档信息

- 文档名称：基础资料与统计分析升级设计
- 文档版本：v1.0.0
- 编写日期：2026-10-11
- 适用范围：后端 Controller/API 开发、前端页面开发、联调、测试验收
- 开发边界：本文只定义设计、接口、前端交互和验收标准；确认后再拆实施计划并开发

## 2. 已确认决策

- 基础资料采用完整主数据闭环，覆盖商品、商品分类、单位、商品单位换算、仓库、供应商、客户。
- 删除策略采用逻辑删除，并且被业务引用的数据禁止删除，只允许停用。
- 后端接口采用统一 REST 风格，基础路径为 `/api/v1/basic/**`。
- 每个基础资料菜单至少提供分页列表、详情、新增、编辑、删除、启停用、下拉选项、Excel 导出。
- 导出采用后端生成 `.xlsx` 文件，按当前查询条件导出全量结果，并配置独立导出权限。
- 统计菜单按业务场景组织，不按图表类型组织；每个统计页面内部支持饼图、柱状图、折线图、表格切换。
- 采购、销售、库存、统计页面需要从基础资料接口选择真实主数据，不再要求用户手输业务 ID。

## 3. 当前项目现状

当前后端已有认证、用户权限、采购、销售、库存、统计 Controller。前端已有 Vue 2 + Element UI 管理端，并挂载了基础资料菜单占位页。

当前 SQL 已有基础资料相关表：

- `sys_warehouse`
- `base_product_category`
- `base_unit`
- `base_product`
- `base_product_unit_convert`
- `base_supplier`
- `base_customer`

当前后端已有 `BaseProduct` 实体和 Mapper，但基础资料模块整体缺少对外 Controller、DTO、VO、Service 能力。采购、销售、库存、统计当前主要使用数字 ID，缺少名称回显、下拉选择和主数据引用校验。

## 4. 版本目标

本版本目标是把系统从“能录单”的进销存原型，升级为“有基础资料、有业务引用、有统计分析、有导出能力”的企业级经营管理系统。

核心目标：

- 建立可维护的主数据体系，让采购、销售、库存、统计共用同一套商品、仓库、供应商、客户、单位、分类资料。
- 基础资料支持完整 CRUD、启停用、引用保护和导出。
- 业务单据录入从 ID 输入升级为可搜索下拉选择，降低录入错误。
- 统计菜单从简单列表图表升级为经营分析中心，支持趋势、占比、排行、结构分析和导出。
- 保持现有 `ApiResponse{code,message,data}`、`PageResponse{list,total}`、`pageNum/pageSize`、JWT 鉴权风格。

## 5. 总体架构

### 5.1 后端模块划分

新增基础资料模块建议放在：

```text
src/main/java/org/liangyou/modules/basic/
  controller/
  dto/
  entity/
  mapper/
  service/
  vo/
```

如果为了复用已有 `modules/product` 下的 `BaseProduct`，可以将商品相关实体迁移或保留在 `product` 包，但对外 API 仍统一归属 `/api/v1/basic/**`。推荐后续统一到 `basic` 包，避免“商品在 product，分类和单位在 basic”的边界分裂。

### 5.2 前端模块划分

前端基础资料页面建议放在：

```text
frontend/src/api/basic/
frontend/src/views/basic/
```

统计分析页面建议放在：

```text
frontend/src/api/statistics/
frontend/src/views/statistics/
frontend/src/components/ChartSwitch/
```

### 5.3 数据流

基础资料维护后，业务模块通过 options 接口读取启用状态的主数据：

- 采购入库选择仓库、供应商、商品、单位。
- 销售出库选择仓库、客户、商品、单位。
- 库存查询按仓库、商品、分类筛选，并回显名称。
- 统计分析按日期、仓库、商品、分类、客户、供应商筛选，并展示名称、占比、排行和趋势。

## 6. 基础资料菜单设计

### 6.1 商品管理

菜单路径：`/basic/product`

业务用途：维护门店销售和采购的商品主数据，是采购、销售、库存、统计的共同维度。

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 否 | 商品 ID |
| productCode | String | 是 | 商品编码，唯一 |
| productName | String | 是 | 商品名称 |
| categoryId | Long | 是 | 商品分类 |
| brandName | String | 否 | 品牌 |
| spec | String | 否 | 规格 |
| barcode | String | 否 | 条码 |
| imageUrl | String | 否 | 图片地址 |
| baseUnitId | Long | 是 | 库存基础单位 |
| shelfLifeDays | Integer | 否 | 保质期天数 |
| status | Integer | 是 | 1 启用，0 停用 |
| remark | String | 否 | 备注 |

页面能力：

- 按商品编码、商品名称、分类、状态查询。
- 新增、编辑、详情、删除、启用、停用、导出。
- 商品表单中分类和基础单位使用下拉选择。
- 商品列表回显分类名称、基础单位名称。
- 停用商品后，历史采购、销售、库存和统计仍能回显名称；新单据不能选择停用商品。
- 删除商品前检查采购明细、销售明细、库存汇总、库存批次、库存流水是否引用。

### 6.2 商品分类

菜单路径：`/basic/category`

业务用途：用于商品归类、统计占比、库存结构分析。

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 否 | 分类 ID |
| categoryName | String | 是 | 分类名称 |
| parentId | Long | 是 | 父分类 ID，根节点为 0 |
| sortNo | Integer | 是 | 排序 |
| status | Integer | 是 | 1 启用，0 停用 |
| remark | String | 否 | 备注 |

页面能力：

- 树形列表展示。
- 新增同级、子级、编辑、详情、删除、启用、停用、导出。
- 停用父分类时，新增或编辑商品不能选择该分类及其停用子分类。
- 删除分类前检查是否存在子分类或商品引用。

### 6.3 单位管理

菜单路径：`/basic/unit`

业务用途：维护斤、袋、箱、桶等计量单位。

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 否 | 单位 ID |
| unitCode | String | 是 | 单位编码，唯一 |
| unitName | String | 是 | 单位名称 |

页面能力：

- 按单位编码、单位名称查询。
- 新增、编辑、详情、删除、导出。
- 删除单位前检查商品基础单位、采购明细单位、销售明细单位、单位换算是否引用。
- 单位表当前 SQL 没有 `status/deleted/version` 字段；本版本建议补齐这些通用字段，保持和其他基础资料一致。

### 6.4 商品单位换算

菜单路径：`/basic/unit-convert`

业务用途：维护商品维度的单位换算关系，例如一箱某品牌大米等于 10 袋，一袋等于 10 斤。采购、销售可以使用业务单位，库存统一归集到基础单位。

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 否 | 换算 ID |
| productId | Long | 是 | 商品 ID |
| fromUnitId | Long | 是 | 源单位 |
| toUnitId | Long | 是 | 目标单位 |
| convertRate | Decimal | 是 | 换算比率 |

页面能力：

- 按商品、源单位、目标单位查询。
- 新增、编辑、详情、删除、导出。
- 同一商品下 `productId + fromUnitId + toUnitId` 唯一。
- `convertRate` 必须大于 0。
- 商品基础单位不可被配置成循环换算链路。
- 单位换算表当前 SQL 没有 `status/deleted/version` 字段；本版本建议补齐这些通用字段。

### 6.5 仓库管理

菜单路径：`/basic/warehouse`

业务用途：维护门店仓库、门店库位或经营点。

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 否 | 仓库 ID |
| warehouseCode | String | 是 | 仓库编码，唯一 |
| warehouseName | String | 是 | 仓库名称 |
| contactName | String | 否 | 联系人 |
| contactPhone | String | 否 | 联系电话 |
| address | String | 否 | 地址 |
| status | Integer | 是 | 1 启用，0 停用 |
| remark | String | 否 | 备注 |

页面能力：

- 按仓库编码、仓库名称、状态查询。
- 新增、编辑、详情、删除、启用、停用、导出。
- 删除仓库前检查用户、采购、销售、库存、批次、流水引用。
- 停用仓库后，新单据不能选择；历史数据仍可查询和回显。

### 6.6 供应商管理

菜单路径：`/basic/supplier`

业务用途：维护采购往来对象，用于采购入库、采购分析和供应商排行。

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 否 | 供应商 ID |
| supplierCode | String | 是 | 供应商编码，唯一 |
| supplierName | String | 是 | 供应商名称 |
| contactName | String | 否 | 联系人 |
| contactPhone | String | 否 | 联系电话 |
| address | String | 否 | 地址 |
| taxNo | String | 否 | 税号 |
| bankName | String | 否 | 开户行 |
| bankAccount | String | 否 | 银行账号 |
| status | Integer | 是 | 1 启用，0 停用 |
| remark | String | 否 | 备注 |

页面能力：

- 按供应商编码、供应商名称、联系人、电话、状态查询。
- 新增、编辑、详情、删除、启用、停用、导出。
- 删除供应商前检查采购单引用。
- 停用供应商后，新采购单不能选择。

### 6.7 客户管理

菜单路径：`/basic/customer`

业务用途：维护销售往来对象，用于销售出库、客户销售排行和销售占比分析。

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 否 | 客户 ID |
| customerCode | String | 是 | 客户编码，唯一 |
| customerName | String | 是 | 客户名称 |
| contactName | String | 否 | 联系人 |
| contactPhone | String | 否 | 联系电话 |
| address | String | 否 | 地址 |
| taxNo | String | 否 | 税号 |
| bankName | String | 否 | 开户行 |
| bankAccount | String | 否 | 银行账号 |
| status | Integer | 是 | 1 启用，0 停用 |
| remark | String | 否 | 备注 |

页面能力：

- 按客户编码、客户名称、联系人、电话、状态查询。
- 新增、编辑、详情、删除、启用、停用、导出。
- 删除客户前检查销售单引用。
- 停用客户后，新销售单不能选择。

## 7. 基础资料接口规范

### 7.1 通用接口形态

每个基础资料资源统一提供：

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 分页列表 | GET | `/api/v1/basic/{resources}` |
| 详情 | GET | `/api/v1/basic/{resources}/{id}` |
| 新增 | POST | `/api/v1/basic/{resources}` |
| 编辑 | PUT | `/api/v1/basic/{resources}/{id}` |
| 删除 | DELETE | `/api/v1/basic/{resources}/{id}` |
| 启停用 | PATCH | `/api/v1/basic/{resources}/{id}/status` |
| 下拉选项 | GET | `/api/v1/basic/{resources}/options` |
| Excel 导出 | GET | `/api/v1/basic/{resources}/export` |

`options` 默认只返回启用且未删除的数据。详情和历史业务回显可以查询停用数据。

### 7.2 资源路径

| 菜单 | resources |
| --- | --- |
| 商品管理 | `products` |
| 商品分类 | `categories` |
| 单位管理 | `units` |
| 商品单位换算 | `unit-converts` |
| 仓库管理 | `warehouses` |
| 供应商管理 | `suppliers` |
| 客户管理 | `customers` |

### 7.3 通用查询参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10，最大建议 100 |
| status | Integer | 否 | 1 启用，0 停用 |
| keyword | String | 否 | 通用关键词，匹配编码、名称、联系人等 |

各资源可以增加自己的筛选项，例如商品的 `categoryId`、仓库的 `warehouseCode`、供应商的 `supplierName`。

### 7.4 下拉选项响应

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "code": "P001",
      "name": "东北大米",
      "status": 1
    }
  ]
}
```

商品 options 建议额外返回：

- `categoryId`
- `categoryName`
- `baseUnitId`
- `baseUnitName`
- `spec`
- `brandName`

这样采购、销售明细选择商品后可自动带出规格和基础单位。

### 7.5 启停用请求

```json
{
  "status": 0
}
```

停用不影响历史数据。新增或编辑业务单据时，只能选择启用状态的主数据。

### 7.6 导出接口

导出接口使用 `GET`，查询参数和列表接口保持一致，返回 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`。

响应头建议：

- `Content-Disposition: attachment; filename="products-20261011.xlsx"`

导出范围：

- 按当前查询条件导出全量结果。
- 不受当前分页参数影响。
- 导出字段和列表主字段保持一致，可额外包含备注、创建时间、更新时间。

## 8. 引用保护规则

删除基础资料前必须检查业务引用。

| 资源 | 引用检查 |
| --- | --- |
| 商品 | 采购明细、销售明细、库存汇总、库存批次、库存流水、单位换算 |
| 商品分类 | 子分类、商品 |
| 单位 | 商品基础单位、采购明细单位、销售明细单位、单位换算 |
| 单位换算 | 当前无历史业务强引用时可删除；未来若业务单据记录换算 ID，则需加入引用检查 |
| 仓库 | 用户、采购单、销售单、库存汇总、库存批次、库存流水 |
| 供应商 | 采购单、统计汇总 |
| 客户 | 销售单、统计汇总 |

被引用时删除接口返回：

```json
{
  "code": 400,
  "message": "basic-data-in-use",
  "data": null
}
```

前端提示：`当前资料已被业务数据引用，不能删除，可停用后禁止新业务继续使用。`

## 9. 权限设计

权限码按资源和操作拆分：

| 菜单 | 权限码前缀 |
| --- | --- |
| 商品管理 | `basic:product` |
| 商品分类 | `basic:category` |
| 单位管理 | `basic:unit` |
| 商品单位换算 | `basic:unit-convert` |
| 仓库管理 | `basic:warehouse` |
| 供应商管理 | `basic:supplier` |
| 客户管理 | `basic:customer` |

每个前缀下建议提供：

- `list`
- `view`
- `create`
- `update`
- `delete`
- `status`
- `export`

示例：

- `basic:product:list`
- `basic:product:create`
- `basic:product:update`
- `basic:product:delete`
- `basic:product:status`
- `basic:product:export`

统计权限码：

- `statistics:dashboard:view`
- `statistics:trend:view`
- `statistics:sale-product:view`
- `statistics:purchase-product:view`
- `statistics:inventory-structure:view`
- `statistics:customer-rank:view`
- `statistics:supplier-rank:view`
- `statistics:category-share:view`
- 每个统计页面导出权限追加 `:export`，例如 `statistics:sale-product:export`。

## 10. 采购、销售、库存联动升级

### 10.1 采购入库

采购入库页面需要升级：

- 仓库使用 `warehouses/options`。
- 供应商使用 `suppliers/options`。
- 商品明细使用 `products/options`。
- 单位使用商品单位和单位换算能力，默认带出商品基础单位。
- 商品选择后回显商品名称、规格、分类、基础单位。
- 保存时后端校验仓库、供应商、商品、单位均存在且启用。
- 列表和详情回显仓库名称、供应商名称、商品名称、单位名称。

### 10.2 销售出库

销售出库页面需要升级：

- 仓库使用 `warehouses/options`。
- 客户使用 `customers/options`。
- 商品明细使用 `products/options`。
- 商品选择后回显规格、分类、基础单位、可用库存。
- 单位可选择商品支持的单位换算。
- 保存时后端校验仓库、客户、商品、单位均存在且启用。
- 列表和详情回显仓库名称、客户名称、商品名称、单位名称。

### 10.3 库存管理

库存页面需要升级：

- 按仓库、商品、分类查询。
- 列表回显仓库名称、商品名称、分类名称、基础单位名称。
- 库存调整时只能选择启用仓库和启用商品。
- 批次库存和库存流水回显商品、仓库、单位、分类名称。
- 支持库存列表、批次列表、流水列表按当前条件 Excel 导出。

## 11. 统计菜单设计

统计菜单按业务场景组织：

```text
统计分析
  经营看板
  进销趋势分析
  商品销售分析
  商品采购分析
  库存结构分析
  客户销售排行
  供应商采购排行
  分类占比分析
```

### 11.1 统计页通用交互

每个统计页面统一包含：

- 筛选区：日期范围、仓库、商品分类、商品、客户、供应商、统计口径。
- 指标卡：展示核心金额、数量、占比、环比预留。
- 图表工具栏：饼图、柱状图、折线图、表格切换。
- 图表区：使用 ECharts。
- 明细表格：展示与图表同源的数据。
- 导出按钮：按当前筛选条件导出 Excel。

图表选择原则：

- 饼图：用于占比，例如商品销售占比、分类库存占比。
- 柱状图：用于排行和对比，例如商品销售 Top、客户销售 Top。
- 折线图：用于趋势，例如每日采购金额、每日销售金额。
- 表格：用于核对明细数据和导出前确认。

### 11.2 经营看板

路径：`/statistics/dashboard`

用途：管理者快速查看经营情况。

展示内容：

- 今日采购金额
- 今日销售金额
- 今日销售数量
- 当前库存商品数
- 库存金额预估
- 销售 Top 5 商品
- 客户销售 Top 5
- 供应商采购 Top 5
- 近 7/30 天进销趋势

接口建议：

- `GET /api/v1/statistics/dashboard`
- `GET /api/v1/statistics/dashboard/export`

### 11.3 进销趋势分析

路径：`/statistics/trend`

用途：分析采购、销售随时间变化的趋势。

维度：

- 日
- 月
- 仓库
- 采购/销售/全部

指标：

- 采购金额
- 销售金额
- 采购数量
- 销售数量

图表：

- 折线图：金额和数量趋势。
- 柱状图：按日期对比采购和销售。
- 表格：日期、采购金额、销售金额、采购数量、销售数量。

接口建议：

- `GET /api/v1/statistics/trends`
- `GET /api/v1/statistics/trends/export`

### 11.4 商品销售分析

路径：`/statistics/sale-product`

用途：体现每份货物的销售占比、销售排行和趋势。

维度：

- 商品
- 商品分类
- 仓库
- 日期范围

指标：

- 销售金额
- 销售数量
- 金额占比
- 数量占比
- 排名

图表：

- 饼图：商品销售金额占比或数量占比。
- 柱状图：商品销售排行。
- 折线图：选中商品的销售趋势。
- 表格：商品编码、商品名称、分类、规格、销售数量、销售金额、占比。

接口建议：

- `GET /api/v1/statistics/sale-products`
- `GET /api/v1/statistics/sale-products/export`

### 11.5 商品采购分析

路径：`/statistics/purchase-product`

用途：体现每份货物的采购占比、采购排行和供应来源。

维度：

- 商品
- 商品分类
- 供应商
- 仓库
- 日期范围

指标：

- 采购金额
- 采购数量
- 金额占比
- 数量占比
- 平均采购单价

图表：

- 饼图：商品采购金额占比。
- 柱状图：商品采购排行。
- 折线图：选中商品的采购趋势。
- 表格：商品编码、商品名称、分类、供应商、采购数量、采购金额、平均单价、占比。

接口建议：

- `GET /api/v1/statistics/purchase-products`
- `GET /api/v1/statistics/purchase-products/export`

### 11.6 库存结构分析

路径：`/statistics/inventory-structure`

用途：查看库存数量、库存价值和分类结构。

维度：

- 仓库
- 商品
- 商品分类

指标：

- 库存数量
- 可用库存
- 锁定库存
- 库存价值预估
- 分类占比

图表：

- 饼图：分类库存占比、商品库存占比。
- 柱状图：库存数量 Top。
- 表格：仓库、商品、分类、库存数量、可用库存、单位、库存价值预估。

接口建议：

- `GET /api/v1/statistics/inventory-structure`
- `GET /api/v1/statistics/inventory-structure/export`

### 11.7 客户销售排行

路径：`/statistics/customer-rank`

用途：查看客户贡献和销售集中度。

维度：

- 客户
- 商品
- 商品分类
- 仓库
- 日期范围

指标：

- 销售金额
- 销售数量
- 客户占比
- 下单次数

图表：

- 饼图：客户销售金额占比。
- 柱状图：客户销售 Top。
- 折线图：选中客户销售趋势。
- 表格：客户编码、客户名称、销售金额、销售数量、下单次数、占比。

接口建议：

- `GET /api/v1/statistics/customer-ranks`
- `GET /api/v1/statistics/customer-ranks/export`

### 11.8 供应商采购排行

路径：`/statistics/supplier-rank`

用途：查看供应商采购贡献和采购集中度。

维度：

- 供应商
- 商品
- 商品分类
- 仓库
- 日期范围

指标：

- 采购金额
- 采购数量
- 供应商占比
- 采购次数

图表：

- 饼图：供应商采购金额占比。
- 柱状图：供应商采购 Top。
- 折线图：选中供应商采购趋势。
- 表格：供应商编码、供应商名称、采购金额、采购数量、采购次数、占比。

接口建议：

- `GET /api/v1/statistics/supplier-ranks`
- `GET /api/v1/statistics/supplier-ranks/export`

### 11.9 分类占比分析

路径：`/statistics/category-share`

用途：查看分类在采购、销售、库存中的占比。

维度：

- 商品分类
- 仓库
- 日期范围
- 统计类型：采购、销售、库存

指标：

- 金额
- 数量
- 占比
- 分类排名

图表：

- 饼图：分类金额占比或数量占比。
- 柱状图：分类排行。
- 折线图：分类趋势。
- 表格：分类名称、金额、数量、占比。

接口建议：

- `GET /api/v1/statistics/category-shares`
- `GET /api/v1/statistics/category-shares/export`

## 12. 统计接口响应规范

统计接口建议统一返回前端可直接渲染的数据结构。

趋势接口响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "metrics": [
      { "key": "purchaseAmount", "name": "采购金额", "value": 12000.00 },
      { "key": "saleAmount", "name": "销售金额", "value": 18000.00 }
    ],
    "rows": [
      {
        "dimensionId": "2026-10-11",
        "dimensionName": "2026-10-11",
        "purchaseAmount": 12000.00,
        "saleAmount": 18000.00,
        "purchaseQty": 500.00,
        "saleQty": 420.00
      }
    ]
  }
}
```

排行和占比接口响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalAmount": 100000.00,
    "totalQty": 3000.00,
    "rows": [
      {
        "dimensionId": 1,
        "dimensionCode": "P001",
        "dimensionName": "东北大米",
        "categoryName": "大米",
        "amount": 30000.00,
        "qty": 800.00,
        "amountRate": 30.00,
        "qtyRate": 26.67,
        "rankNo": 1
      }
    ]
  }
}
```

## 13. 前端页面规范

### 13.1 列表页布局

基础资料列表页统一结构：

- 查询区：使用紧凑表单，一行展示高频筛选，更多筛选可折叠。
- 工具栏：新增、导出、刷新。
- 表格区：固定主字段，状态使用标签。
- 行操作：详情、编辑、启停用、删除。
- 分页：使用当前已有 `Pagination` 组件。

### 13.2 表单交互

- 新增和编辑使用弹窗或抽屉，字段较多的供应商、客户可使用抽屉。
- 编码、名称、关联字段必须校验。
- 唯一编码冲突由后端返回明确错误，例如 `product-code-exists`。
- 删除和停用必须二次确认。
- 被引用禁止删除时，前端展示后端返回的业务提示。

### 13.3 统计页面交互

- 图表类型使用按钮组或标签页切换。
- 饼图支持金额占比和数量占比切换。
- 柱状图默认按金额倒序展示 Top 10，可切换 Top 20。
- 折线图用于趋势，不适合无时间维度的数据时隐藏。
- 表格和导出使用同一筛选条件。

## 14. 数据库调整建议

当前 `base_unit` 和 `base_product_unit_convert` 缺少企业级通用字段。建议补充：

```sql
ALTER TABLE base_unit
  ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0停用',
  ADD COLUMN remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  ADD COLUMN updated_by BIGINT DEFAULT NULL,
  ADD COLUMN updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0,
  ADD COLUMN version INT NOT NULL DEFAULT 0;

ALTER TABLE base_product_unit_convert
  ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0停用',
  ADD COLUMN remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  ADD COLUMN updated_by BIGINT DEFAULT NULL,
  ADD COLUMN updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0,
  ADD COLUMN version INT NOT NULL DEFAULT 0;
```

如需支持库存价值分析，建议库存统计使用现有批次 `unit_price` 和 `remain_qty` 计算，不在本版本新增成本核算表。

## 15. 错误码建议

| 场景 | message |
| --- | --- |
| 基础资料不存在 | `basic-data-not-found` |
| 基础资料已被引用 | `basic-data-in-use` |
| 编码重复 | `{resource}-code-exists` |
| 名称为空 | `{resource}-name-required` |
| 状态非法 | `status-invalid` |
| 换算比率非法 | `convert-rate-invalid` |
| 主数据已停用 | `basic-data-disabled` |
| 导出失败 | `export-failed` |

资源错误码示例：

- `product-code-exists`
- `warehouse-code-exists`
- `supplier-code-exists`
- `customer-code-exists`
- `unit-code-exists`

## 16. 测试与验收标准

### 16.1 后端验收

- 每个基础资料 Controller 都具备列表、详情、新增、编辑、删除、启停用、options、export 接口。
- 所有新增和编辑接口具备必填校验和唯一编码校验。
- 删除被业务引用的数据时返回 `basic-data-in-use`。
- 停用后的资料不会出现在 options 接口中。
- 历史采购、销售、库存、统计仍能回显停用资料名称。
- Excel 导出按筛选条件导出全量数据。
- 统计接口能返回名称、金额、数量、占比和排行。

### 16.2 前端验收

- 基础资料占位页全部替换为真实页面。
- 所有基础资料菜单支持查询、新增、编辑、删除、启停用、导出。
- 采购、销售页面不再要求用户手输仓库、供应商、客户、商品、单位 ID。
- 库存页面支持按仓库、商品、分类查询并回显名称。
- 统计分析菜单按本文定义的业务场景展示。
- 每个统计页面支持至少饼图、柱状图、表格；有时间维度的页面支持折线图。
- 图表数据和表格数据同源，切换图表不重新解释口径。
- 导出文件内容与当前筛选条件一致。

## 17. 后续实施顺序建议

确认本文后，建议按以下顺序拆实施计划：

1. 补齐基础资料实体、DTO、VO、Mapper、Service、Controller 和测试。
2. 补齐 Excel 导出公共工具和基础资料导出。
3. 前端实现基础资料 API 和 7 个基础资料页面。
4. 升级采购、销售、库存页面的主数据选择和名称回显。
5. 升级统计接口，补齐场景化统计数据结构。
6. 重构统计前端菜单和图表切换组件。
7. 进行端到端联调和验收。

