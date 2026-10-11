<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="仓库ID"><el-input-number v-model="query.warehouseId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="商品ID"><el-input-number v-model="query.productId" :min="1" controls-position="right" /></el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar"><span /> <RightToolbar @queryTable="load" /></div>
      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="warehouseId" label="仓库ID" />
        <el-table-column prop="productId" label="商品ID" />
        <el-table-column prop="totalQty" label="总库存" />
        <el-table-column prop="lockedQty" label="锁定数量" />
        <el-table-column prop="availableQty" label="可用库存" />
        <el-table-column label="操作" width="190">
          <template slot-scope="{ row }">
            <el-button v-permission="'inventory:stock:update'" type="text" @click="open(row)">调整</el-button>
            <el-button type="text" @click="$router.push(`/inventory/batch?productId=${row.productId}&warehouseId=${row.warehouseId}`)">批次</el-button>
            <el-button v-permission="'inventory:stock:delete'" type="text" class="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />
    </el-card>
    <el-dialog title="库存调整" :visible.sync="dialogVisible" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="总库存"><el-input-number v-model="form.totalQty" :min="0" /></el-form-item>
        <el-form-item label="可用库存"><el-input-number v-model="form.availableQty" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { listInventory, updateInventory, deleteInventory } from '@/api/inventory/inventory'
import Pagination from '@/components/Pagination'
import RightToolbar from '@/components/RightToolbar'

export default {
  name: 'InventoryStock',
  components: { Pagination, RightToolbar },
  data() {
    return { query: { pageNum: 1, pageSize: 10 }, list: [], total: 0, dialogVisible: false, form: {} }
  },
  created() { this.load() },
  methods: {
    async load() {
      const data = await listInventory(this.query)
      this.list = data.list || []
      this.total = data.total || 0
    },
    reset() { this.query = { pageNum: 1, pageSize: 10 }; this.load() },
    open(row) { this.form = { ...row, remark: '' }; this.dialogVisible = true },
    async save() {
      await updateInventory(this.form.id, { totalQty: this.form.totalQty, availableQty: this.form.availableQty, remark: this.form.remark })
      this.dialogVisible = false
      this.load()
    },
    async remove(row) {
      await this.$confirm(`确认删除库存记录 ${row.id}？`, '提示')
      await deleteInventory(row.id)
      this.load()
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
.danger { color: #f56c6c; }
</style>
