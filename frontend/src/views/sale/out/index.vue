<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="单号"><el-input v-model="query.orderNo" clearable /></el-form-item>
        <el-form-item label="仓库ID"><el-input-number v-model="query.warehouseId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="客户ID"><el-input-number v-model="query.customerId" :min="1" controls-position="right" /></el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-permission="'sale:order:create'" type="primary" @click="open()">新增</el-button>
        <RightToolbar @queryTable="load" />
      </div>
      <el-table :data="list" border>
        <el-table-column prop="orderNo" label="出库单号" min-width="170" />
        <el-table-column prop="warehouseId" label="仓库ID" />
        <el-table-column prop="customerId" label="客户ID" />
        <el-table-column prop="bizDate" label="业务日期" />
        <el-table-column prop="totalAmount" label="总金额" />
        <el-table-column label="操作" width="220">
          <template slot-scope="{ row }">
            <el-button type="text" @click="$router.push(`/sale/out/detail/${row.id}`)">详情</el-button>
            <el-button v-permission="'sale:order:update'" type="text" @click="open(row)">编辑</el-button>
            <el-button v-permission="'sale:order:delete'" type="text" class="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />
    </el-card>
    <el-dialog :title="form.id ? '编辑销售出库' : '新增销售出库'" :visible.sync="dialogVisible" width="880px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="仓库ID"><el-input-number v-model="form.warehouseId" :min="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="客户ID"><el-input-number v-model="form.customerId" :min="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="业务日期"><el-date-picker v-model="form.bizDate" value-format="yyyy-MM-dd" type="date" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-button size="mini" @click="addItem">新增明细</el-button>
        <el-table :data="form.items" border class="items">
          <el-table-column label="商品ID"><template slot-scope="{ row }"><el-input-number v-model="row.productId" :min="1" /></template></el-table-column>
          <el-table-column label="单位ID"><template slot-scope="{ row }"><el-input-number v-model="row.unitId" :min="1" /></template></el-table-column>
          <el-table-column label="数量"><template slot-scope="{ row }"><el-input-number v-model="row.qty" :min="0" /></template></el-table-column>
          <el-table-column label="单价"><template slot-scope="{ row }"><el-input-number v-model="row.unitPrice" :min="0" /></template></el-table-column>
          <el-table-column label="金额"><template slot-scope="{ row }"><el-input-number v-model="row.amount" :min="0" /></template></el-table-column>
          <el-table-column label="操作" width="70"><template slot-scope="{ $index }"><el-button type="text" class="danger" @click="form.items.splice($index, 1)">删除</el-button></template></el-table-column>
        </el-table>
      </el-form>
      <span slot="footer"><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { listSaleOut, getSaleOut, addSaleOut, updateSaleOut, deleteSaleOut } from '@/api/sale/saleOut'
import Pagination from '@/components/Pagination'
import RightToolbar from '@/components/RightToolbar'

export default {
  name: 'SaleOut',
  components: { Pagination, RightToolbar },
  data() {
    return { query: { pageNum: 1, pageSize: 10 }, list: [], total: 0, dialogVisible: false, form: { items: [] } }
  },
  created() { this.load() },
  methods: {
    async load() {
      const data = await listSaleOut(this.query)
      this.list = data.list || []
      this.total = data.total || 0
    },
    reset() { this.query = { pageNum: 1, pageSize: 10 }; this.load() },
    async open(row) {
      this.form = row ? await getSaleOut(row.id) : { warehouseId: 1, customerId: 1, bizDate: new Date().toISOString().slice(0, 10), status: 1, items: [] }
      if (!this.form.items) this.form.items = []
      this.dialogVisible = true
    },
    addItem() {
      this.form.items.push({ productId: 1, unitId: 1, qty: 1, unitPrice: 0, amount: 0 })
    },
    async save() {
      if (this.form.id) await updateSaleOut(this.form.id, this.form)
      else await addSaleOut(this.form)
      this.dialogVisible = false
      this.load()
    },
    async remove(row) {
      await this.$confirm(`确认删除销售单 ${row.orderNo}？`, '提示')
      await deleteSaleOut(row.id)
      this.load()
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
.items { margin-top: 12px; }
.danger { color: #f56c6c; }
</style>
