<template>
  <div class="sale-editor">
    <el-page-header :content="form.id ? '编辑销售订单' : '销售开单'" @back="$router.push('/sale/out')" />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card shadow="never" v-loading="loading">
      <el-form ref="orderForm" :model="form" label-width="105px" :disabled="saving">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="销售类型"><el-radio-group v-model="form.saleType"><el-radio-button label="retail">零售</el-radio-button><el-radio-button label="wholesale">批发</el-radio-button></el-radio-group></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="交付方式"><el-radio-group v-model="form.fulfillmentType"><el-radio-button label="pickup">自提</el-radio-button><el-radio-button label="delivery">配送</el-radio-button></el-radio-group></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="业务日期"><el-date-picker v-model="form.bizDate" type="date" value-format="yyyy-MM-dd" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="仓库" required><BasicDataSelect v-model="form.warehouseId" resource="warehouses" :selected-label="form.warehouseName" @change="refreshAvailability" /></el-form-item></el-col>
          <el-col :span="16"><el-form-item label="客户"><el-radio-group v-model="customerMode" @change="modeChanged"><el-radio label="guest">散客</el-radio><el-radio label="registered">正式客户</el-radio></el-radio-group>
            <el-select v-if="customerMode === 'registered'" v-model="form.customerId" filterable remote clearable :remote-method="findCustomers" placeholder="按名称、编码或手机号搜索" style="width:55%;margin-left:12px" @change="customerChanged">
              <el-option v-for="item in customers" :key="item.id" :value="item.id" :label="`${item.customerName} · ${item.contactPhone || '无电话'}`" />
            </el-select>
            <el-button v-if="customerMode === 'registered'" v-permission="'basic:customer:create'" type="text" @click="quickVisible=true">新增客户</el-button>
          </el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="客户称呼"><el-input v-model.trim="form.customerName" maxlength="128" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="经手人"><el-input v-model.trim="form.handlerName" maxlength="64" /></el-form-item></el-col>
        </el-row>
        <div v-if="form.fulfillmentType === 'delivery'" class="delivery-fields">
          <h3>配送信息</h3>
          <el-row :gutter="16">
            <el-col :span="8"><el-form-item label="收货人" required><el-input v-model.trim="form.contactName" maxlength="64" /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="收货电话" required><el-input v-model.trim="form.contactPhone" maxlength="32" /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="计划送达"><el-date-picker v-model="form.plannedDeliveryTime" type="datetime" value-format="yyyy-MM-ddTHH:mm:ss" /></el-form-item></el-col>
          </el-row>
          <el-form-item label="配送地址" required><el-input v-model.trim="form.deliveryAddress" maxlength="255" /></el-form-item>
          <el-form-item label="配送备注"><el-input v-model.trim="form.deliveryRemark" type="textarea" maxlength="255" show-word-limit /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model.trim="form.remark" maxlength="255" /></el-form-item>
        <div class="item-head"><h3>商品明细</h3><el-button size="small" @click="addItem">新增商品</el-button></div>
        <el-table :data="form.items" border>
          <el-table-column label="商品" min-width="205"><template slot-scope="{ row }"><BasicDataSelect v-model="row.productId" resource="products" :selected-label="row.productName" @change="(id, option) => productChanged(row, option)" /><small>{{ row.spec || '' }}</small></template></el-table-column>
          <el-table-column label="单位" min-width="145"><template slot-scope="{ row }"><BasicDataSelect v-model="row.unitId" resource="product-units" :product-id="row.productId" :selected-label="row.unitName" @change="(id, option) => unitChanged(row, option)" /></template></el-table-column>
          <el-table-column label="数量" width="135"><template slot-scope="{ row }"><el-input-number v-model="row.qty" :min="0.0001" :precision="4" controls-position="right" /></template></el-table-column>
          <el-table-column label="单价" width="135"><template slot-scope="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="4" controls-position="right" /></template></el-table-column>
          <el-table-column label="金额" width="105"><template slot-scope="{ row }">{{ (Number(row.qty || 0) * Number(row.unitPrice || 0)).toFixed(2) }}</template></el-table-column>
          <el-table-column label="可售 / 所需基础数量" width="170"><template slot-scope="{ row }">{{ row.availableQty == null ? '—' : row.availableQty }} / {{ row.convertRate ? (Number(row.qty) * Number(row.convertRate)).toFixed(4) : '—' }}</template></el-table-column>
          <el-table-column label="备注" min-width="120"><template slot-scope="{ row }"><el-input v-model.trim="row.remark" maxlength="255" /></template></el-table-column>
          <el-table-column label="操作" width="70"><template slot-scope="{ $index }"><el-button type="text" @click="form.items.splice($index,1)">移除</el-button></template></el-table-column>
        </el-table>
        <p class="total">本单应收：¥ {{ amount }}</p>
        <div class="actions"><el-button :loading="saving" @click="save(false)">保存订单</el-button><el-button v-if="!form.id || form.orderStatus === 'draft'" v-permission="'sale:order:confirm'" type="primary" :loading="saving" @click="save(true)">确认订单</el-button></div>
      </el-form>
    </el-card>
    <el-dialog title="快捷新增客户" :visible.sync="quickVisible" width="480px" append-to-body>
      <el-input v-model.trim="quick.customerName" placeholder="客户名称（必填）" @input="findDuplicates" /><el-input v-model.trim="quick.contactName" placeholder="联系人" /><el-input v-model.trim="quick.contactPhone" placeholder="手机号" @input="findDuplicates" /><el-input v-model.trim="quick.address" placeholder="地址" />
      <el-alert v-if="duplicates.length" :title="`发现相似客户：${duplicates.map(c => c.customerName).join('、')}`" type="warning" :closable="false" />
      <span slot="footer"><el-button @click="quickVisible=false">取消</el-button><el-button type="primary" @click="createCustomer">保存客户</el-button></span>
    </el-dialog>
  </div>
</template>
<script>
import BasicDataSelect from '@/components/BasicDataSelect'
import { listInventory } from '@/api/inventory/inventory'
import { getSaleOut, addSaleOut, updateSaleOut, saleAction, searchCustomers, quickCustomer } from '@/api/sale/saleOut'
import { blankSaleOrder, validateSaleOrder, salePayload, totalAmount, copySaleOrder } from './saleOrderForm'
export default {
  name: 'SaleOrderEditor', components: { BasicDataSelect },
  data: () => ({ form: blankSaleOrder(), customerMode: 'guest', customers: [], duplicates: [], quickVisible: false,
    quick: { customerName: '', contactName: '', contactPhone: '', address: '' }, loading: false, saving: false, error: '' }),
  computed: { amount() { return totalAmount(this.form.items) } },
  async created() {
    const copy = this.$route.query && this.$route.query.copy
    const id = this.$route.params.id || copy
    if (!id) return
    this.loading = true
    try {
      const order = await getSaleOut(id)
      this.form = { ...(copy ? copySaleOrder(order) : { ...blankSaleOrder(), ...order }), items: order.items.map(item => ({ ...item, id: copy ? undefined : item.id, availableQty: null, convertRate: item.baseQty && item.qty ? Number(item.baseQty) / Number(item.qty) : 1 })) }
      this.customerMode = order.customerId ? 'registered' : 'guest'
      if (order.customerId) this.customers = [{ id: order.customerId, customerName: order.customerName, contactPhone: order.contactPhone }]
      this.refreshAvailability()
    } catch (e) { this.error = e.message || '订单加载失败' } finally { this.loading = false }
  },
  methods: {
    addItem() { this.form.items.push({ productId: null, unitId: null, qty: 1, unitPrice: 0, remark: '', availableQty: null, convertRate: 1 }) },
    modeChanged(mode) { if (mode === 'guest') { Object.assign(this.form, { customerId: null, customerName: '散客', contactName: '', contactPhone: '', deliveryAddress: '' }) } else this.findCustomers('') },
    async findCustomers(keyword) { try { this.customers = await searchCustomers(keyword || '') } catch (e) { this.error = e.message } },
    customerChanged(id) {
      const found = this.customers.find(item => item.id === id)
      if (!found) return
      if (this.form.deliveryAddress && this.form.deliveryAddress !== found.address) this.$message.warning('本单收货信息已按所选客户重新带入')
      Object.assign(this.form, { customerName: found.customerName, contactName: found.contactName || '', contactPhone: found.contactPhone || '', deliveryAddress: found.address || '' })
    },
    async findDuplicates() { this.duplicates = this.quick.contactPhone || this.quick.customerName ? await searchCustomers(this.quick.contactPhone || this.quick.customerName) : [] },
    async createCustomer() {
      try {
        const customer = await quickCustomer(this.quick)
        this.customers = [customer, ...this.customers]
        this.form.customerId = customer.id; this.customerChanged(customer.id)
        this.quickVisible = false
      } catch (e) { this.error = e.message || '新增客户失败' }
    },
    productChanged(row, option) {
      row.productName = option ? option.name : ''; row.spec = option ? option.spec : ''
      row.unitId = option ? option.baseUnitId : null; row.unitName = option ? option.baseUnitName : ''
      row.convertRate = 1; this.refreshAvailability()
    },
    unitChanged(row, option) { row.unitName = option ? option.name : ''; row.convertRate = option ? Number(option.convertRate || 1) : 1 },
    async refreshAvailability() {
      if (!this.form.warehouseId) return
      await Promise.all(this.form.items.filter(row => row.productId).map(async row => {
        try { const result = await listInventory({ warehouseId: this.form.warehouseId, productId: row.productId, pageNum: 1, pageSize: 1 }); row.availableQty = (result.list || [])[0]?.availableQty ?? 0 }
        catch (e) { row.availableQty = null }
      }))
    },
    async save(confirm) {
      if (this.customerMode === 'registered' && !this.form.customerId) { this.error = '请选择正式客户，或切换为散客'; return }
      const message = validateSaleOrder(this.form)
      if (message) { this.error = message; return }
      this.saving = true; this.error = ''
      try {
        const id = this.form.id
        const savedId = id ? (await updateSaleOut(id, salePayload(this.form)), id) : await addSaleOut(salePayload(this.form))
        this.form.id = savedId
        if (!id) this.form.orderStatus = 'draft'
        if (confirm) await saleAction(savedId, 'confirm')
        this.$router.push(`/sale/out/detail/${savedId}`)
      } catch (e) { this.error = e.message || '保存失败' } finally { this.saving = false }
    }
  }
}
</script>
<style scoped>
.sale-editor { padding: 16px; }
.el-card { margin-top: 16px; }
.delivery-fields { padding: 8px 16px; background: #f7f9fc; margin: 12px 0; }
.item-head, .actions { display: flex; justify-content: space-between; align-items: center; margin: 12px 0; }
.total { text-align: right; font-size: 18px; font-weight: bold; }
.el-dialog .el-input { margin: 5px 0; }
</style>
