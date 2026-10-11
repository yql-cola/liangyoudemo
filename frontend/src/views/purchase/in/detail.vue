<template>
  <el-card shadow="never">
    <div slot="header"><el-button icon="el-icon-back" @click="$router.back()">返回</el-button> 采购详情</div>
    <el-descriptions :column="3" border>
      <el-descriptions-item label="单号">{{ detail.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="仓库ID">{{ detail.warehouseId }}</el-descriptions-item>
      <el-descriptions-item label="供应商ID">{{ detail.supplierId }}</el-descriptions-item>
      <el-descriptions-item label="业务日期">{{ detail.bizDate }}</el-descriptions-item>
      <el-descriptions-item label="总金额">{{ detail.totalAmount }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ detail.remark }}</el-descriptions-item>
    </el-descriptions>
    <el-table :data="detail.items || []" border class="items">
      <el-table-column prop="productId" label="商品ID" />
      <el-table-column prop="unitId" label="单位ID" />
      <el-table-column prop="qty" label="数量" />
      <el-table-column prop="unitPrice" label="单价" />
      <el-table-column prop="amount" label="金额" />
      <el-table-column prop="batchNo" label="批次号" />
      <el-table-column prop="inboundTime" label="入库时间" />
    </el-table>
  </el-card>
</template>

<script>
import { getPurchaseIn } from '@/api/purchase/purchaseIn'
export default {
  name: 'PurchaseInDetail',
  data() { return { detail: {} } },
  async created() { this.detail = await getPurchaseIn(this.$route.params.id) }
}
</script>

<style scoped>
.items { margin-top: 16px; }
</style>
