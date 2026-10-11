<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="商品ID"><el-input-number v-model="query.productId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="仓库ID"><el-input-number v-model="query.warehouseId" :min="1" controls-position="right" /></el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <el-table :data="list" border>
        <el-table-column prop="id" label="批次ID" />
        <el-table-column prop="warehouseId" label="仓库ID" />
        <el-table-column prop="productId" label="商品ID" />
        <el-table-column prop="batchNo" label="批次号" />
        <el-table-column prop="inboundTime" label="入库时间" />
        <el-table-column prop="productionDate" label="生产日期" />
        <el-table-column prop="expiryDate" label="过期日期" />
        <el-table-column prop="unitPrice" label="进价" />
        <el-table-column prop="initQty" label="初始数量" />
        <el-table-column prop="remainQty" label="剩余数量" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { listInventoryBatches } from '@/api/inventory/inventory'

export default {
  name: 'InventoryBatch',
  data() {
    return { query: { productId: Number(this.$route.query.productId) || undefined, warehouseId: Number(this.$route.query.warehouseId) || undefined }, list: [] }
  },
  created() { if (this.query.productId) this.load() },
  methods: {
    async load() {
      if (!this.query.productId) {
        this.$message.warning('请先输入商品ID')
        return
      }
      this.list = await listInventoryBatches(this.query.productId, { warehouseId: this.query.warehouseId })
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
</style>
