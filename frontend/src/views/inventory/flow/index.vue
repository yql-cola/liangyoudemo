<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="仓库ID"><el-input-number v-model="query.warehouseId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="商品ID"><el-input-number v-model="query.productId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="query.bizTimeFrom" value-format="yyyy-MM-ddTHH:mm:ss" type="datetime" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="query.bizTimeTo" value-format="yyyy-MM-ddTHH:mm:ss" type="datetime" /></el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="query = {}; load()">重置</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <el-table :data="list" border>
        <el-table-column prop="id" label="流水ID" />
        <el-table-column prop="warehouseId" label="仓库ID" />
        <el-table-column prop="productId" label="商品ID" />
        <el-table-column prop="batchId" label="批次ID" />
        <el-table-column prop="flowType" label="流水类型" />
        <el-table-column prop="bizType" label="业务类型" />
        <el-table-column prop="bizId" label="业务单ID" />
        <el-table-column prop="changeQty" label="变化数量" />
        <el-table-column prop="afterQty" label="变化后数量" />
        <el-table-column prop="bizTime" label="业务时间" min-width="160" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { listInventoryFlows } from '@/api/inventory/inventory'

export default {
  name: 'InventoryFlow',
  data() { return { query: {}, list: [] } },
  created() { this.load() },
  methods: {
    async load() {
      this.list = await listInventoryFlows(this.query)
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
</style>
