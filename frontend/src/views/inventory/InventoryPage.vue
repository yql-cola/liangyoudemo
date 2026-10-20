<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query" @submit.native.prevent="search">
        <el-form-item label="仓库"><BasicDataSelect v-model="query.warehouseId" resource="warehouses" @change="search" /></el-form-item>
        <el-form-item label="商品"><BasicDataSelect v-model="query.productId" resource="products" @change="search" /></el-form-item>
        <el-form-item label="分类"><BasicDataSelect v-model="query.categoryId" resource="categories" @change="search" /></el-form-item>
        <template v-if="kind === 'flow'">
          <el-form-item label="开始时间"><el-date-picker v-model="query.bizTimeFrom" type="datetime" value-format="yyyy-MM-ddTHH:mm:ss" /></el-form-item>
          <el-form-item label="结束时间"><el-date-picker v-model="query.bizTimeTo" type="datetime" value-format="yyyy-MM-ddTHH:mm:ss" /></el-form-item>
        </template>
        <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar"><span>{{ title }} · 数量均为商品基础数量</span><div><el-button v-permission="`inventory:${kind}:export`" :loading="exporting" @click="exportData">导出 Excel</el-button><el-button :loading="loading" icon="el-icon-refresh" @click="load">刷新</el-button></div></div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="productName" label="商品" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="110" />
        <el-table-column prop="categoryName" label="分类" min-width="110" />
        <el-table-column prop="unitName" label="基础单位" min-width="100" />
        <el-table-column v-for="column in columns" :key="column.prop" :prop="column.prop" :label="column.label" :min-width="column.width || 130">
          <template slot-scope="{ row }">{{ displayCell(column.prop, row[column.prop]) }}</template>
        </el-table-column>
        <el-table-column v-if="kind === 'stock'" label="操作" width="190" fixed="right"><template slot-scope="{ row }">
          <el-button v-permission="'inventory:stock:update'" type="text" @click="open(row)">调整</el-button>
          <el-button v-permission="'inventory:batch:list'" type="text" @click="$router.push({ path: '/inventory/batch', query: { productId: row.productId, warehouseId: row.warehouseId } })">批次</el-button>
          <el-button v-permission="'inventory:stock:delete'" type="text" class="danger" @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <Pagination v-if="kind === 'stock'" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />
    </el-card>
    <el-dialog v-if="kind === 'stock'" title="库存调整" :visible.sync="dialogVisible" width="540px" :close-on-click-modal="false" :before-close="closeDialog">
      <p>{{ form.warehouseName }} · {{ form.productName }} · {{ form.spec }} · 基础单位：{{ form.unitName }}</p>
      <el-alert title="调整会同步批次库存；锁定数量保持不变。总库存 = 可用库存 + 锁定数量。" type="info" :closable="false" />
      <el-alert v-if="formError" :title="formError" type="error" :closable="false" show-icon />
      <el-form :model="form" :disabled="saving" label-width="125px">
        <el-form-item label="总基础数量" required><el-input-number v-model="form.totalQty" :min="0" :precision="4" /></el-form-item>
        <el-form-item label="可用基础数量" required><el-input-number v-model="form.availableQty" :min="0" :precision="4" /></el-form-item>
        <el-form-item label="锁定基础数量"><span>{{ lockedQty }} {{ form.unitName }}（保持不变）</span></el-form-item>
        <el-form-item label="调整原因" required><el-input v-model.trim="form.remark" type="textarea" maxlength="255" show-word-limit /></el-form-item>
      </el-form>
      <span slot="footer"><el-button :disabled="saving" @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></span>
    </el-dialog>
  </div>
</template>
<script>
import * as api from '@/api/inventory/inventory'
import BasicDataSelect from '@/components/BasicDataSelect'
import Pagination from '@/components/Pagination'
import { downloadFile } from '@/utils/download'
import { validDate } from '@/views/purchase/in/orderForm'
import { validateAdjustment } from './adjustment'
import { inventoryTypeLabel } from './inventoryTypes'

const columns = {
  stock: [{ prop: 'totalQty', label: '总基础数量' }, { prop: 'availableQty', label: '可用基础数量' }, { prop: 'lockedQty', label: '锁定基础数量' }, { prop: 'inDeliveryQty', label: '配送中占用量' }],
  batch: [{ prop: 'batchNo', label: '批次号' }, { prop: 'inboundTime', label: '入库时间', width: 180 }, { prop: 'productionDate', label: '生产日期' }, { prop: 'expiryDate', label: '过期日期' }, { prop: 'unitPrice', label: '基础单位进价' }, { prop: 'initQty', label: '初始基础数量' }, { prop: 'remainQty', label: '剩余基础数量' }],
  flow: [{ prop: 'flowType', label: '流水类型' }, { prop: 'bizType', label: '业务类型' }, { prop: 'bizId', label: '业务单据编号' }, { prop: 'changeQty', label: '变动基础数量' }, { prop: 'afterQty', label: '变动后基础数量' }, { prop: 'bizTime', label: '业务时间', width: 180 }, { prop: 'remark', label: '备注' }]
}
export default {
  name: 'InventoryPage',
  components: { BasicDataSelect, Pagination },
  props: { kind: { type: String, required: true } },
  data: () => ({ query: {}, list: [], total: 0, loading: false, exporting: false, error: '', requestId: 0, form: {}, lockedQty: 0, dialogVisible: false, formError: '', saving: false }),
  computed: { title() { return { stock: '库存汇总', batch: '批次库存', flow: '库存流水' }[this.kind] }, columns() { return columns[this.kind] } },
  created() { this.query = this.initialQuery(true); this.load() },
  beforeDestroy() { this.requestId++ },
  watch: { '$route.query': { deep: true, handler() { this.query = this.initialQuery(true); this.load() } } },
  methods: {
    displayCell(prop, value) { return prop === 'flowType' || prop === 'bizType' ? inventoryTypeLabel(value) : value },
    initialQuery(route = false) {
      const query = { warehouseId: undefined, productId: undefined, categoryId: undefined }
      if (this.kind === 'stock') Object.assign(query, { pageNum: 1, pageSize: 10 })
      if (this.kind === 'flow') Object.assign(query, { bizTimeFrom: undefined, bizTimeTo: undefined })
      if (route && this.$route) for (const key of ['warehouseId', 'productId', 'categoryId']) query[key] = Number(this.$route.query[key]) || undefined
      return query
    },
    validateQuery() {
      const { bizTimeFrom: from, bizTimeTo: to } = this.query
      if ((from && !validDate(from, true)) || (to && !validDate(to, true))) return '请选择有效的业务时间'
      return from && to && from > to ? '开始时间不能晚于结束时间' : ''
    },
    async load() {
      const id = ++this.requestId
      this.error = this.validateQuery()
      if (this.error) { this.loading = false; return }
      this.loading = true
      try {
        const fn = this.kind === 'stock' ? api.listInventory : this.kind === 'batch' ? api.listInventoryBatches : api.listInventoryFlows
        const data = await fn({ ...this.query })
        if (id === this.requestId) { this.list = Array.isArray(data) ? data : data.list || []; this.total = data.total || 0 }
      } catch (e) { if (id === this.requestId) this.error = e.message || '库存加载失败' }
      finally { if (id === this.requestId) this.loading = false }
    },
    search() { if (this.kind === 'stock') this.query.pageNum = 1; return this.load() },
    reset() { this.query = this.initialQuery(); return this.load() },
    open(row) { this.form = { ...row, remark: '' }; this.lockedQty = row.lockedQty; this.formError = ''; this.dialogVisible = true },
    closeDialog(done) { if (!this.saving) done() },
    async save() {
      if (this.saving) return
      this.formError = validateAdjustment({ ...this.form, lockedQty: this.lockedQty })
      if (this.formError) return
      this.saving = true
      try { await api.updateInventory(this.form.id, { totalQty: this.form.totalQty, availableQty: this.form.availableQty, remark: this.form.remark.trim() }); this.dialogVisible = false; this.$message.success('库存调整成功'); await this.load() }
      catch (e) { this.formError = e.message || '库存调整失败' }
      finally { this.saving = false }
    },
    async remove(row) {
      try { await this.$confirm(`确认删除 ${row.warehouseName} 的 ${row.productName} 库存？`, '删除确认', { type: 'warning' }) } catch (e) { return }
      try { await api.deleteInventory(row.id); await this.load() } catch (e) { this.error = e.message || '删除失败' }
    },
    async exportData() {
      if (this.exporting) return
      this.error = this.validateQuery(); if (this.error) return
      const params = { ...this.query }; delete params.pageNum; delete params.pageSize
      this.exporting = true
      try { await downloadFile(await api.exportInventory(this.kind, params), `${this.title}.xlsx`) }
      catch (e) { this.error = e.message || '导出失败' }
      finally { this.exporting = false }
    }
  }
}
</script>
<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.el-alert { margin-bottom: 16px; }
.danger { color: #f56c6c; }
</style>
