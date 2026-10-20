<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query" @submit.native.prevent="search">
        <el-form-item label="单号"><el-input v-model.trim="query.orderNo" clearable @keyup.enter.native="search" /></el-form-item>
        <el-form-item label="仓库"><BasicDataSelect v-model="query.warehouseId" resource="warehouses" @change="search" /></el-form-item>
        <el-form-item :label="partnerLabel"><BasicDataSelect v-model="query[partnerKey]" :resource="partnerResource" @change="search" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar"><el-button v-permission="`${kind}:order:create`" type="primary" @click="open()">新增{{ title }}</el-button><el-button icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button></div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="orderNo" label="单号" min-width="180" /><el-table-column prop="warehouseName" label="仓库" />
        <el-table-column :prop="partnerNameKey" :label="partnerLabel" /><el-table-column prop="bizDate" label="业务日期" /><el-table-column prop="totalAmount" label="总金额" />
        <el-table-column label="操作" width="200"><template slot-scope="{ row }">
          <el-button v-permission="`${kind}:order:view`" type="text" @click="$router.push(`${basePath}/detail/${row.id}`)">详情</el-button>
          <el-button v-if="canEdit" type="text" @click="open(row)">编辑</el-button>
          <el-button v-permission="`${kind}:order:delete`" type="text" class="danger" @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <Pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />
    </el-card>
    <el-dialog :title="`${form.id ? '编辑' : '新增'}${title}`" :visible.sync="dialogVisible" width="95%" :close-on-click-modal="false" :before-close="closeDialog">
      <el-alert v-if="formError" :title="formError" type="error" :closable="false" show-icon />
      <el-form v-loading="detailLoading" :model="form" label-width="85px" :disabled="saving || detailLoading">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="仓库" required><BasicDataSelect v-model="form.warehouseId" resource="warehouses" :selected-label="form.warehouseName" :disabled="saving || detailLoading" @change="warehouseChanged" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item :label="partnerLabel" required><BasicDataSelect v-model="form[partnerKey]" :resource="partnerResource" :selected-label="form[partnerNameKey]" :disabled="saving || detailLoading" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="业务日期" required><el-date-picker v-model="form.bizDate" type="date" value-format="yyyy-MM-dd" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model.trim="form.remark" maxlength="255" /></el-form-item>
        <el-button type="primary" plain :disabled="saving || detailLoading" @click="addItem">新增明细</el-button>
        <el-table :data="form.items" border class="items">
          <el-table-column label="商品 / 规格" min-width="230"><template slot-scope="{ row }">
            <BasicDataSelect v-model="row.productId" resource="products" :selected-label="row.productName" :disabled="saving || detailLoading" @change="(value, option) => productChanged(row, option)" />
            <div class="hint">{{ row.spec || '—' }} · {{ row.categoryName || '—' }} · 基础单位：{{ row.baseUnitName || row.stockUnitName || '—' }}</div>
          </template></el-table-column>
          <el-table-column label="业务单位" min-width="180"><template slot-scope="{ row }">
            <BasicDataSelect v-model="row.unitId" resource="product-units" :product-id="row.productId" :selected-label="row.unitName" :disabled="!row.productId || saving || detailLoading" @change="(value, option) => unitChanged(row, option)" />
            <div v-if="row.convertRate" class="hint">1 {{ row.unitName }} = {{ row.convertRate }} {{ row.baseUnitName || row.stockUnitName || '基础单位' }}</div>
          </template></el-table-column>
          <el-table-column label="数量" min-width="150"><template slot-scope="{ row }"><el-input-number v-model="row.qty" :min="0.0001" :precision="4" controls-position="right" /></template></el-table-column>
          <el-table-column label="业务单位单价" min-width="150"><template slot-scope="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="4" controls-position="right" /></template></el-table-column>
          <el-table-column label="金额" min-width="100"><template slot-scope="{ row }">{{ amount(row.qty, row.unitPrice) }}</template></el-table-column>
          <el-table-column v-if="kind === 'sale'" label="当前可用基础数量" min-width="190"><template slot-scope="{ row }">
            <span>{{ row.stockLoading ? '查询中…' : row.stockError || (row.availableQty === null ? '请选择仓库和商品' : `${row.availableQty} ${row.stockUnitName || row.baseUnitName || '基础单位'}`) }}</span>
            <div v-if="row.convertRate" class="hint">本行出库 {{ Number((row.qty * row.convertRate).toFixed(4)) }} {{ row.stockUnitName || row.baseUnitName || '基础单位' }}</div>
          </template></el-table-column>
          <el-table-column v-if="kind === 'purchase'" label="批次号" min-width="150"><template slot-scope="{ row }"><el-input v-model.trim="row.batchNo" maxlength="64" /></template></el-table-column>
          <el-table-column v-if="kind === 'purchase'" label="入库时间" min-width="220"><template slot-scope="{ row }"><el-date-picker v-model="row.inboundTime" type="datetime" value-format="yyyy-MM-ddTHH:mm:ss" /></template></el-table-column>
          <el-table-column v-if="kind === 'purchase'" label="生产 / 过期日期" min-width="180"><template slot-scope="{ row }"><el-date-picker v-model="row.productionDate" type="date" value-format="yyyy-MM-dd" placeholder="生产日期" /><el-date-picker v-model="row.expiryDate" type="date" value-format="yyyy-MM-dd" placeholder="过期日期" /></template></el-table-column>
          <el-table-column label="操作" width="65" fixed="right"><template slot-scope="{ $index }"><el-button type="text" class="danger" :disabled="saving" @click="form.items.splice($index, 1)">移除</el-button></template></el-table-column>
        </el-table>
        <p v-if="kind === 'sale'" class="hint">可用量和出库量均按商品基础单位展示。编辑单据时可用量为当前库存，最终可售数量由保存时的库存校验确定。</p>
      </el-form>
      <span slot="footer"><el-button :disabled="saving" @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" :disabled="detailLoading || detailFailed" @click="save">保存</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import * as purchaseApi from '@/api/purchase/purchaseIn'
import * as saleApi from '@/api/sale/saleOut'
import { getProductUnits } from '@/api/basic'
import { listInventory } from '@/api/inventory/inventory'
import BasicDataSelect from '@/components/BasicDataSelect'
import Pagination from '@/components/Pagination'
import { orderAmount, validateOrder, buildOrderPayload, localDateTime } from './orderForm'
import { hasPermission } from '@/utils/permission'

export default {
  name: 'BusinessOrderPage',
  components: { BasicDataSelect, Pagination },
  props: { kind: { type: String, required: true } },
  data: () => ({ query: { pageNum: 1, pageSize: 10 }, list: [], total: 0, loading: false, error: '', formError: '', dialogVisible: false,
    form: { items: [] }, saving: false, detailLoading: false, detailFailed: false, requestId: 0, detailId: 0 }),
  computed: {
    // Editing loads full detail, so both operation permissions are required.
    canEdit() { return ['update', 'view'].every(action => hasPermission(this.$store.state.user.permissions, `${this.kind}:order:${action}`)) },
    title() { return this.kind === 'purchase' ? '采购入库' : '销售出库' },
    partnerKey() { return this.kind === 'purchase' ? 'supplierId' : 'customerId' },
    partnerNameKey() { return this.kind === 'purchase' ? 'supplierName' : 'customerName' },
    partnerResource() { return this.kind === 'purchase' ? 'suppliers' : 'customers' },
    partnerLabel() { return this.kind === 'purchase' ? '供应商' : '客户' },
    basePath() { return this.kind === 'purchase' ? '/purchase/in' : '/sale/out' },
    api() {
      return this.kind === 'purchase'
        ? { list: purchaseApi.listPurchaseIn, get: purchaseApi.getPurchaseIn, create: purchaseApi.addPurchaseIn, update: purchaseApi.updatePurchaseIn, remove: purchaseApi.deletePurchaseIn }
        : { list: saleApi.listSaleOut, get: saleApi.getSaleOut, create: saleApi.addSaleOut, update: saleApi.updateSaleOut, remove: saleApi.deleteSaleOut }
    }
  },
  created() { this.load() },
  beforeDestroy() { this.requestId++; this.detailId++ },
  methods: {
    amount: orderAmount,
    async load() {
      const id = ++this.requestId; this.loading = true; this.error = ''
      try { const data = await this.api.list({ ...this.query }); if (id === this.requestId) { this.list = data.list || []; this.total = data.total || 0 } }
      catch (e) { if (id === this.requestId) this.error = e.message || '单据加载失败' }
      finally { if (id === this.requestId) this.loading = false }
    },
    search() { this.query.pageNum = 1; return this.load() },
    reset() { this.query = { pageNum: 1, pageSize: 10 }; return this.load() },
    itemDefaults(row = {}) {
      return { productId: undefined, unitId: undefined, productName: '', unitName: '', spec: '', categoryName: '', baseUnitName: '', baseUnitId: undefined,
        convertRate: undefined, qty: 1, unitPrice: 0, remark: '', batchNo: '', inboundTime: localDateTime(), productionDate: null, expiryDate: null,
        availableQty: null, stockUnitName: '', stockLoading: false, stockError: '', stockRequestId: 0, ...row }
    },
    async open(row) {
      const id = ++this.detailId
      this.formError = ''; this.dialogVisible = true; this.detailLoading = true; this.detailFailed = false
      this.form = { warehouseId: undefined, [this.partnerKey]: undefined, warehouseName: '', [this.partnerNameKey]: '', bizDate: localDateTime().slice(0, 10), status: 1, remark: '', items: [] }
      try {
        const detail = row ? await this.api.get(row.id) : {}
        if (id !== this.detailId) return
        this.form = { ...this.form, ...detail, items: (detail.items || []).map(item => this.itemDefaults(item)) }
        if (this.kind === 'sale') this.form.items.forEach(item => this.refreshStock(item))
        this.form.items.forEach(item => this.hydrateUnit(item))
      } catch (e) { if (id === this.detailId) { this.formError = e.message || '单据详情加载失败'; this.detailFailed = true } }
      finally { if (id === this.detailId) this.detailLoading = false }
    },
    addItem() { this.form.items.push(this.itemDefaults()) },
    async hydrateUnit(row) {
      const productId = row.productId, unitId = row.unitId
      if (!productId) return
      try {
        const options = await getProductUnits(productId)
        if (row.productId !== productId || row.unitId !== unitId) return
        const unit = options.find(item => item.id === unitId), base = options.find(item => item.id === item.baseUnitId)
        if (unit) { row.convertRate = unit.convertRate; row.baseUnitId = unit.baseUnitId }
        if (base) row.baseUnitName = base.name
      } catch (e) { /* Historical rows keep their supplied product/unit names. Save performs authoritative validation. */ }
    },
    productChanged(row, product) {
      Object.assign(row, { productName: product ? product.name : '', spec: product ? product.spec || '' : '', categoryName: product ? product.categoryName || '' : '',
        baseUnitId: product ? product.baseUnitId : undefined, baseUnitName: product ? product.baseUnitName || '' : '',
        unitId: product ? product.baseUnitId : undefined, unitName: product ? product.baseUnitName || '' : '', convertRate: product ? 1 : undefined })
      this.refreshStock(row)
    },
    unitChanged(row, unit) { row.unitName = unit ? unit.name : ''; row.convertRate = unit ? unit.convertRate : undefined; if (unit) row.baseUnitId = unit.baseUnitId },
    warehouseChanged() { if (this.kind === 'sale') this.form.items.forEach(row => this.refreshStock(row)) },
    async refreshStock(row) {
      const id = ++row.stockRequestId
      row.availableQty = null; row.stockError = ''; row.stockUnitName = ''
      if (this.kind !== 'sale' || !this.form.warehouseId || !row.productId) { row.stockLoading = false; return }
      row.stockLoading = true
      try {
        const result = await listInventory({ pageNum: 1, pageSize: 10, warehouseId: this.form.warehouseId, productId: row.productId })
        if (id !== row.stockRequestId) return
        row.availableQty = (result.list || []).reduce((sum, item) => sum + Number(item.availableQty || 0), 0)
        row.stockUnitName = result.list && result.list[0] ? result.list[0].unitName || '' : row.baseUnitName
      } catch (e) { if (id === row.stockRequestId) row.stockError = e.message || '库存查询失败' }
      finally { if (id === row.stockRequestId) row.stockLoading = false }
    },
    closeDialog(done) { if (!this.saving) { this.detailId++; done() } },
    async save() {
      if (this.saving || this.detailLoading || this.detailFailed) return
      this.formError = validateOrder(this.form, this.kind)
      if (this.formError) return
      this.saving = true
      try {
        const payload = buildOrderPayload(this.form, this.kind)
        if (this.form.id) await this.api.update(this.form.id, payload)
        else await this.api.create(payload)
        this.dialogVisible = false; this.$message.success('保存成功'); await this.load()
      } catch (e) { this.formError = e.message || '保存失败' }
      finally { this.saving = false }
    },
    async remove(row) {
      try { await this.$confirm(`确认删除单据 ${row.orderNo}？`, '删除确认', { type: 'warning' }) } catch (e) { return }
      try { await this.api.remove(row.id); this.$message.success('删除成功'); await this.load() } catch (e) { this.error = e.message || '删除失败' }
    }
  }
}
</script>
<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
.items, .el-alert { margin-top: 12px; margin-bottom: 12px; }
.hint { color: #606266; font-size: 12px; line-height: 1.8; }
.danger { color: #f56c6c; }
.el-input-number, .el-date-editor { width: 100%; }
</style>
