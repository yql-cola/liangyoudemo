<template>
  <el-card v-loading="loading" shadow="never">
    <div slot="header"><el-button icon="el-icon-back" @click="$router.back()">返回</el-button> {{ kind === 'purchase' ? '采购入库' : '销售出库' }}详情 <el-button type="text" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-descriptions :column="3" border>
      <el-descriptions-item label="单号">{{ detail.orderNo }}</el-descriptions-item><el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
      <el-descriptions-item :label="kind === 'purchase' ? '供应商' : '客户'">{{ kind === 'purchase' ? detail.supplierName : detail.customerName }}</el-descriptions-item>
      <el-descriptions-item label="业务日期">{{ detail.bizDate }}</el-descriptions-item><el-descriptions-item label="总金额">{{ detail.totalAmount }}</el-descriptions-item><el-descriptions-item label="备注">{{ detail.remark }}</el-descriptions-item>
    </el-descriptions>
    <el-table :data="detail.items || []" border class="items">
      <el-table-column prop="productName" label="商品" min-width="130" /><el-table-column prop="spec" label="规格" /><el-table-column prop="categoryName" label="分类" />
      <el-table-column prop="unitName" label="业务单位" /><el-table-column prop="qty" label="业务数量" /><el-table-column prop="baseQty" label="基础数量" />
      <el-table-column prop="unitPrice" label="业务单位单价" /><el-table-column prop="amount" label="金额" />
      <template v-if="kind === 'purchase'"><el-table-column prop="batchNo" label="批次号" /><el-table-column prop="productionDate" label="生产日期" /><el-table-column prop="expiryDate" label="过期日期" /><el-table-column prop="inboundTime" label="入库时间" min-width="170" /></template>
      <el-table-column prop="remark" label="备注" />
    </el-table>
  </el-card>
</template>
<script>
import { getPurchaseIn } from '@/api/purchase/purchaseIn'
import { getSaleOut } from '@/api/sale/saleOut'
export default {
  name: 'BusinessOrderDetail', props: { kind: { type: String, required: true } },
  data: () => ({ detail: {}, loading: false, error: '', requestId: 0 }),
  created() { this.load() },
  watch: { '$route.params.id'() { this.load() } },
  beforeDestroy() { this.requestId++ },
  methods: { async load() {
    const id = ++this.requestId; this.loading = true; this.error = ''
    try { const detail = await (this.kind === 'purchase' ? getPurchaseIn : getSaleOut)(this.$route.params.id); if (id === this.requestId) this.detail = detail }
    catch (e) { if (id === this.requestId) this.error = e.message || '详情加载失败' }
    finally { if (id === this.requestId) this.loading = false }
  } }
}
</script>
<style scoped>.items { margin-top: 16px; }.el-alert { margin-bottom: 16px; }</style>
