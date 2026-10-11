# 粮油进销存 Vue 前端设计

## 1. 设计目标

基于当前 Spring Boot 后端接口，设计一套类似 `RuoYi-Vue/ruoyi-ui` 的后台管理前端。前端定位为粮油门店进销存管理系统，优先覆盖当前后端已实现的认证、用户权限、采购入库、销售出库、库存、统计功能。

本设计只规划前端工程和页面，不修改后端接口。后端暂未实现的商品、仓库、客户、供应商、单位、发票、账务等接口，只作为预留模块，不在一期页面中做真实业务闭环。

## 2. 技术架构

参考 RuoYi-Vue 的成熟后台结构，建议前端采用以下技术栈：

- Vue 2.6.x
- Vue CLI 4.x
- Vue Router 3.x
- Vuex 3.x
- Element UI 2.x
- Axios
- ECharts
- Sass

选择 Vue2 + Element UI 的原因是 RuoYi-Vue 原项目就是这个体系，代码结构、组件模式、权限守卫、动态路由都可以直接复用思路，开发成本最低。

如果后续希望技术栈更现代，可以另起 Vue3 + Vite + Element Plus 版本，但这会偏离当前指定的 RuoYi-Vue 架构。

## 3. 推荐工程目录

建议在当前后端项目下新增独立前端目录：

```text
liangyoudemo/
  frontend/
    package.json
    vue.config.js
    .env.development
    .env.production
    src/
      api/
        auth.js
        system/
          user.js
          role.js
          menu.js
        purchase/
          purchaseIn.js
        sale/
          saleOut.js
        inventory/
          inventory.js
        statistics/
          statistics.js
      assets/
      components/
        Pagination/
        RightToolbar/
        DictTag/
      layout/
      router/
        index.js
      store/
        modules/
          user.js
          permission.js
          settings.js
      utils/
        request.js
        auth.js
        permission.js
      views/
        login.vue
        dashboard/
          index.vue
        system/
          user/index.vue
          role/index.vue
          menu/index.vue
        purchase/
          in/index.vue
          in/detail.vue
        sale/
          out/index.vue
          out/detail.vue
        inventory/
          stock/index.vue
          batch/index.vue
          flow/index.vue
          adjust/index.vue
        statistics/
          daily.vue
          monthly.vue
          customer.vue
          supplier.vue
          category.vue
        basic/
          product/index.vue
          warehouse/index.vue
          supplier/index.vue
          customer/index.vue
        error/
          401.vue
          404.vue
```

说明：

- `api/` 按后端业务模块拆分，和 `/api/v1/**` 接口保持一一对应。
- `views/` 按菜单拆分，每个独立业务一个菜单目录。
- `layout/`、`store/`、`permission.js`、`utils/request.js` 参考 RuoYi-Vue。
- `basic/` 目录仅作为后续扩展预留，一期不挂载到可用菜单，或展示“后端接口待实现”。

## 4. 后端接口适配规则

当前后端统一返回结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页结构：

```json
{
  "list": [],
  "total": 0
}
```

前端 Axios 响应拦截规则：

- `code === 0`：返回 `data`
- `code === 401`：清理 token，跳转登录页
- 其他 `code`：弹出 `message`
- `GET` 请求使用 `params`
- `POST/PUT` 请求使用 JSON body
- 请求头统一携带 `Authorization: Bearer ${token}`

由于 RuoYi-Vue 默认成功码通常是 `200`，这里必须改成识别当前后端的 `0`。

## 5. 登录与权限流程

### 5.1 登录流程

页面：`views/login.vue`

调用：

- `POST /api/v1/auth/login`

后端返回：

- `token`
- `tokenType`
- `expiresIn`
- `userInfo.id`
- `userInfo.username`
- `userInfo.realName`
- `userInfo.roles`
- `userInfo.permissions`

前端处理：

- token 保存到 Cookie 或 localStorage
- 用户信息保存到 Vuex
- 登录成功后进入首页 `/index`

### 5.2 当前用户

调用：

- `GET /api/v1/auth/me`

用途：

- 页面刷新后恢复用户信息
- 恢复角色和权限
- 生成动态菜单

### 5.3 退出登录

调用：

- `POST /api/v1/auth/logout`

处理：

- 后端写入 token 注销记录
- 前端清理本地 token 和 Vuex 状态
- 跳转登录页

## 6. 菜单设计

前端菜单按业务连贯性设计，每个独立业务单独一个菜单。

### 6.1 首页

菜单：`首页`

页面：`/index`

展示内容：

- 今日采购金额
- 今日销售金额
- 当前库存 SKU 数
- 库存预警占位
- 近 7 天进销趋势图
- 快捷入口：采购入库、销售出库、库存查询

接口来源：

- 统计接口
- 库存接口

### 6.2 系统管理

菜单：`系统管理`

子菜单：

- 用户管理：`/system/user`
- 角色管理：`/system/role`
- 菜单权限：`/system/menu`

对应接口：

- `/api/v1/users`
- `/api/v1/roles`
- `/api/v1/menus/tree`

页面能力：

- 用户列表、查询、新增、编辑、删除
- 用户分配角色
- 用户授权/撤权
- 角色列表、查询、新增、编辑、删除
- 角色分配权限
- 菜单树查看

注意：

- 当前后端没有“菜单新增/编辑/删除”接口，因此菜单权限页面一期只做树形展示和权限选择辅助。

### 6.3 采购管理

菜单：`采购管理`

子菜单：

- 采购入库：`/purchase/in`

对应接口：

- `POST /api/v1/purchase-ins`
- `PUT /api/v1/purchase-ins/{id}`
- `GET /api/v1/purchase-ins`
- `GET /api/v1/purchase-ins/{id}`
- `DELETE /api/v1/purchase-ins/{id}`

页面能力：

- 采购入库单列表
- 按单号、仓库、供应商、状态、业务日期查询
- 新增采购入库单
- 编辑采购入库单
- 查看采购明细
- 删除采购入库单

业务连贯性：

- 新增采购入库后，后端会自动增加库存、生成批次、生成库存流水。
- 删除采购入库时，若该批次已被销售消耗，后端会返回 `purchase-in-already-consumed`，前端提示“该采购批次已被销售使用，不能删除”。

### 6.4 销售管理

菜单：`销售管理`

子菜单：

- 销售出库：`/sale/out`

对应接口：

- `POST /api/v1/sale-outs`
- `PUT /api/v1/sale-outs/{id}`
- `GET /api/v1/sale-outs`
- `GET /api/v1/sale-outs/{id}`
- `DELETE /api/v1/sale-outs/{id}`

页面能力：

- 销售出库单列表
- 按单号、仓库、客户、状态、业务日期查询
- 新增销售出库单
- 编辑销售出库单
- 查看销售明细
- 删除销售出库单

业务连贯性：

- 新增销售出库时，后端会按批次先进先出扣减库存。
- 库存不足时后端返回 `insufficient-stock`，前端提示“库存不足，请检查库存批次”。
- 删除销售出库后，后端会回滚批次库存和总库存。

### 6.5 库存管理

菜单：`库存管理`

子菜单：

- 库存查询：`/inventory/stock`
- 批次库存：`/inventory/batch`
- 库存流水：`/inventory/flow`
- 库存调整：`/inventory/adjust`

对应接口：

- `GET /api/v1/inventories`
- `GET /api/v1/inventories/{productId}/batches`
- `GET /api/v1/inventories/flows`
- `PUT /api/v1/inventories/{id}`
- `DELETE /api/v1/inventories/{id}`

页面能力：

- 按仓库、商品查询库存
- 查看商品批次、入库时间、生产日期、过期日期、进价、初始数量、剩余数量
- 查看库存流水，包括采购入库、销售出库、库存调整
- 手动调整库存数量并填写备注

业务连贯性：

- 采购入库和销售出库产生的库存变化，都能在库存流水中查询。
- 手动库存调整也会生成库存流水，便于追溯。

### 6.6 统计报表

菜单：`统计报表`

子菜单：

- 日进销统计：`/statistics/daily`
- 月进销统计：`/statistics/monthly`
- 客户销售统计：`/statistics/customer`
- 供应商采购统计：`/statistics/supplier`
- 分类金额统计：`/statistics/category`

对应接口：

- `GET /api/v1/statistics/daily/inbound-outbound`
- `GET /api/v1/statistics/monthly/inbound-outbound`
- `GET /api/v1/statistics/amount-by-customer`
- `GET /api/v1/statistics/amount-by-supplier`
- `GET /api/v1/statistics/amount-by-category`

页面能力：

- 查询条件：仓库、统计类型、日期范围、月份范围
- 表格展示
- ECharts 折线图、柱状图
- 金额和数量汇总卡片

业务连贯性：

- 从采购、销售、库存流水沉淀到统计视图。
- 统计接口当前部分依赖汇总表，部分支持从业务单据回退计算，前端只按接口返回展示。

### 6.7 基础资料

菜单：`基础资料`

子菜单预留：

- 商品管理：`/basic/product`
- 仓库管理：`/basic/warehouse`
- 供应商管理：`/basic/supplier`
- 客户管理：`/basic/customer`
- 单位管理：`/basic/unit`
- 商品分类：`/basic/category`

当前状态：

- 数据库已有相关表。
- 后端目前没有对应 Controller。
- 一期建议不开放该菜单，或开放只读占位页，提示“后端接口待实现”。

前端后续需要这些接口，否则采购和销售页面中的商品、仓库、供应商、客户只能手动输入 ID，体验不完整。

## 7. 页面级设计

### 7.1 列表页标准结构

所有业务列表页统一采用 RuoYi 风格：

- 顶部查询表单
- 操作按钮区
- 数据表格
- 分页组件
- 新增/编辑弹窗
- 详情抽屉或详情页

适用页面：

- 用户管理
- 角色管理
- 采购入库
- 销售出库
- 库存查询

### 7.2 单据页面结构

采购入库和销售出库属于单据型业务，建议采用“主表 + 明细表”结构。

主表字段：

- 仓库 ID
- 供应商 ID 或客户 ID
- 业务日期
- 状态
- 备注

明细表字段：

- 商品 ID
- 单位 ID
- 数量
- 单价
- 金额
- 批次号
- 生产日期
- 过期日期
- 入库时间
- 备注

销售明细不需要批次号，后端会自动按库存批次扣减。

### 7.3 库存流水页面

库存流水是业务追溯核心页面。

展示字段：

- 仓库 ID
- 商品 ID
- 批次 ID
- 流水类型
- 业务类型
- 业务单 ID
- 业务明细 ID
- 变化数量
- 变化后数量
- 业务时间
- 备注

支持从采购单详情、销售单详情、库存列表跳转到对应库存流水。

## 8. 前端 API 模块设计

### 8.1 auth.js

```js
login(data)
getCurrentUser()
logout()
```

### 8.2 system/user.js

```js
listUser(params)
getUser(id)
addUser(data)
updateUser(id, data)
deleteUser(id)
assignUserRoles(id, data)
assignUserPermissions(id, data)
getUserPermissions(id)
```

### 8.3 system/role.js

```js
listRole(params)
addRole(data)
updateRole(id, data)
deleteRole(id)
assignRolePermissions(id, data)
```

### 8.4 system/menu.js

```js
getMenuTree()
```

### 8.5 purchase/purchaseIn.js

```js
listPurchaseIn(params)
getPurchaseIn(id)
addPurchaseIn(data)
updatePurchaseIn(id, data)
deletePurchaseIn(id)
```

### 8.6 sale/saleOut.js

```js
listSaleOut(params)
getSaleOut(id)
addSaleOut(data)
updateSaleOut(id, data)
deleteSaleOut(id)
```

### 8.7 inventory/inventory.js

```js
listInventory(params)
listInventoryBatches(productId, params)
listInventoryFlows(params)
updateInventory(id, data)
deleteInventory(id)
```

### 8.8 statistics/statistics.js

```js
dailyInboundOutbound(params)
monthlyInboundOutbound(params)
amountByCustomer(params)
amountBySupplier(params)
amountByCategory(params)
```

## 9. 路由与权限设计

路由建议分为：

- 常量路由：登录页、首页、401、404
- 动态业务路由：根据角色和权限控制菜单

当前后端 `/auth/me` 已返回 roles 和 permissions。前端可以按 RuoYi 的 `permission.js` 模式生成动态路由。

一期也可以先采用固定菜单 + 前端权限指令控制按钮，降低开发复杂度。

推荐策略：

- 页面菜单先固定，保证业务能跑通。
- 按钮级权限读取 `permissions`，后续再切换为后端菜单树动态生成。

原因：

- 当前后端有菜单树接口，但没有菜单维护接口。
- 固定菜单更适合当前接口状态。
- 后续权限体系完善后，可以平滑切换到动态菜单。

## 10. 权限编码建议

前端按钮权限建议按业务规划，和后端 `permissions` 字段对齐：

```text
system:user:list
system:user:add
system:user:edit
system:user:delete
system:user:assignRole
system:user:assignPermission

system:role:list
system:role:add
system:role:edit
system:role:delete
system:role:assignPermission

purchase:in:list
purchase:in:add
purchase:in:edit
purchase:in:delete
purchase:in:detail

sale:out:list
sale:out:add
sale:out:edit
sale:out:delete
sale:out:detail

inventory:stock:list
inventory:stock:edit
inventory:batch:list
inventory:flow:list

statistics:daily:list
statistics:monthly:list
statistics:customer:list
statistics:supplier:list
statistics:category:list
```

实际编码应以数据库 `sys_permission.permission_code` 为准。前端先按以上编码预留。

## 11. 关键业务流程

### 11.1 采购入库到库存

1. 用户进入采购入库页面。
2. 新增采购单，填写仓库、供应商、业务日期和商品明细。
3. 前端调用 `POST /api/v1/purchase-ins`。
4. 后端创建采购单、采购明细、库存、批次、库存流水。
5. 前端跳转采购详情或刷新列表。
6. 用户可到库存查询、批次库存、库存流水中查看结果。

### 11.2 销售出库到库存扣减

1. 用户进入销售出库页面。
2. 新增销售单，填写仓库、客户、业务日期和商品明细。
3. 前端调用 `POST /api/v1/sale-outs`。
4. 后端校验库存，按批次先进先出扣减。
5. 库存不足时前端展示业务错误。
6. 成功后刷新销售列表和库存页面。

### 11.3 库存调整到流水追溯

1. 用户进入库存查询页面。
2. 点击调整库存。
3. 填写总数量、可用数量、备注。
4. 前端调用 `PUT /api/v1/inventories/{id}`。
5. 后端更新库存并生成库存调整流水。
6. 前端可跳转库存流水页面查看调整记录。

## 12. 一期实现范围

一期建议实现：

- 登录页
- 首页
- 用户管理
- 角色管理
- 菜单树查看
- 采购入库
- 销售出库
- 库存查询
- 批次库存
- 库存流水
- 库存调整
- 统计报表

一期暂不实现：

- 商品管理
- 仓库管理
- 供应商管理
- 客户管理
- 单位管理
- 商品分类管理
- 发票管理
- 独立账务管理
- 文件上传、导入导出
- 字典管理
- 操作日志
- 定时任务

这些能力需要后端先补齐接口。

## 13. 待后端补齐的接口

为了让前端体验完整，建议后端后续补齐：

- 商品分类 CRUD
- 商品 CRUD
- 单位 CRUD
- 仓库 CRUD
- 供应商 CRUD
- 客户 CRUD
- 商品选择下拉接口
- 仓库选择下拉接口
- 供应商选择下拉接口
- 客户选择下拉接口
- 权限菜单维护接口
- 发票管理接口
- 账务收支接口

其中商品、仓库、供应商、客户是采购和销售页面最优先需要的基础接口。

## 14. 开发顺序建议

1. 搭建 `frontend/` 工程骨架。
2. 迁移 RuoYi 风格 layout、登录、权限守卫、request 封装。
3. 对接登录、当前用户、退出登录。
4. 做首页和固定菜单。
5. 做用户、角色、菜单权限页面。
6. 做采购入库页面。
7. 做销售出库页面。
8. 做库存查询、批次、流水、调整页面。
9. 做统计报表页面。
10. 补齐错误提示、权限按钮、页面跳转和基础联调。

## 15. 风险与约束

- 当前后端用户新增接口直接保存明文密码，前端不应假设已经加密。
- 当前后端基础资料接口缺失，采购/销售页面一期只能输入 ID 或使用临时静态选项。
- 当前后端返回成功码是 `0`，不能直接套用 RuoYi 默认 `200` 判断。
- 当前后端没有统一导出接口，一期不设计导出按钮。
- 当前后端没有商品名称、仓库名称等联查返回，前端列表可能只能展示 ID。

## 16. 总结

推荐先做一套 RuoYi-Vue 风格的轻量后台前端，采用固定菜单、统一 request 封装、Vuex 用户状态、Element UI 表格表单组件。页面围绕当前后端已实现业务闭环展开：采购入库增加库存，销售出库扣减库存，库存流水做追溯，统计报表做经营查看。

这套设计能先把当前后端能力完整用起来，同时为商品、仓库、供应商、客户、发票、账务等后续模块预留菜单和目录。
