<template>
  <div class="sale-list">
    <el-card shadow="never">
      <el-form :inline="true" :model="query" @submit.native.prevent="load">
        <el-form-item label="订单编号"><el-input v-model.trim="query.orderNo" clearable /></el-form-item>
        <el-form-item label="客户"><el-input v-model.trim="query.customerName" clearable /></el-form-item>
        <el-form-item label="销售类型"><el-select v-model="query.saleType" clearable><el-option label="零售" value="retail" /><el-option label="批发" value="wholesale" /></el-select></el-form-item>
        <el-form-item label="交付方式"><el-select v-model="query.fulfillmentType" clearable><el-option label="自提" value="pickup" /><el-option label="配送" value="delivery" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.orderStatus" clearable><el-option v-for="(name, code) in states" :key="code" :label="name" :value="code" /></el-select></el-form-item>
        <el-form-item label="录单日期"><el-date-picker v-model="query.bizDateFrom" type="date" value-format="yyyy-MM-dd" placeholder="开始" /> 至 <el-date-picker v-model="query.bizDateTo" type="date" value-format="yyyy-MM-dd" placeholder="结束" /></el-form-item>
        <el-form-item label="完成日期"><el-date-picker v-model="query.completedDateFrom" type="date" value-format="yyyy-MM-dd" placeholder="开始" /> 至 <el-date-picker v-model="query.completedDateTo" type="date" value-format="yyyy-MM-dd" placeholder="结束" /></el-form-item>
        <el-form-item><el-checkbox v-model="query.guest">仅散客</el-checkbox></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar"><el-button v-permission="'sale:order:create'" type="primary" @click="$router.push('/sale/new')">销售开单</el-button><el-button @click="load">刷新</el-button></div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="orderNo" label="订单编号" min-width="185" />
        <el-table-column prop="customerName" label="客户" min-width="110" />
        <el-table-column label="类型" width="80"><template slot-scope="{ row }">{{ row.saleType === 'wholesale' ? '批发' : '零售' }}</template></el-table-column>
        <el-table-column label="交付" width="80"><template slot-scope="{ row }">{{ row.fulfillmentType === 'delivery' ? '配送' : '自提' }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template slot-scope="{ row }">{{ states[row.orderStatus] || row.orderStatus }}</template></el-table-column>
        <el-table-column prop="bizDate" label="录单日期" width="112" /><el-table-column prop="completedTime" label="完成时间" min-width="150" />
        <el-table-column prop="totalAmount" label="本单应收" width="110" />
        <el-table-column label="操作" width="200"><template slot-scope="{ row }"><el-button v-permission="'sale:order:view'" type="text" @click="$router.push(`/sale/out/detail/${row.id}`)">详情</el-button><el-button v-if="['draft','pending_handoff','pending_delivery'].includes(row.orderStatus)" v-permission="'sale:order:update'" type="text" @click="$router.push(`/sale/out/edit/${row.id}`)">编辑</el-button></template></el-table-column>
      </el-table>
      <Pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />
    </el-card>
  </div>
</template>
<script>
import Pagination from '@/components/Pagination'
import { listSaleOut } from '@/api/sale/saleOut'
export const states = { draft: '草稿', pending_handoff: '待交付', pending_delivery: '待配送', in_delivery: '配送中', awaiting_return: '待退回', completed: '已完成', cancelled: '已取消', returned: '已退回' }
export default {
  name: 'SaleOrderList', components: { Pagination },
  data: () => ({ states, query: { pageNum: 1, pageSize: 10, guest: false }, rows: [], total: 0, loading: false, error: '' }),
  created() { this.load() },
  methods: {
    async load() { this.loading = true; this.error = ''; try { const data = await listSaleOut(this.query); this.rows = data.list || []; this.total = data.total || 0 } catch (e) { this.error = e.message || '订单加载失败' } finally { this.loading = false } },
    search() { this.query.pageNum = 1; this.load() },
    reset() { this.query = { pageNum: 1, pageSize: 10, guest: false }; this.load() }
  }
}
</script>
<style scoped>.sale-list { padding: 16px; }.el-card { margin-bottom: 16px; }.toolbar { margin-bottom: 12px; }</style>
