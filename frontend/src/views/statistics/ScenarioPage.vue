<template>
  <div class="page">
    <el-card shadow="never">
      <div slot="header" class="heading"><strong>{{ config.title }}</strong><el-tag v-if="inventory" type="info">当前库存快照</el-tag></div>
      <el-form :inline="true" :model="query" @submit.native.prevent="load">
        <template v-if="!inventory">
          <el-form-item label="开始日期"><el-date-picker v-model="query.dateFrom" type="date" value-format="yyyy-MM-dd" /></el-form-item>
          <el-form-item label="结束日期"><el-date-picker v-model="query.dateTo" type="date" value-format="yyyy-MM-dd" /></el-form-item>
        </template>
        <el-form-item label="仓库"><BasicDataSelect v-model="query.warehouseId" resource="warehouses" /></el-form-item>
        <el-form-item label="商品"><BasicDataSelect v-model="query.productId" resource="products" /></el-form-item>
        <el-form-item label="分类"><BasicDataSelect v-model="query.categoryId" resource="categories" /></el-form-item>
        <el-form-item v-if="!inventory && config.customers && query.statType !== 'purchase'" label="客户"><BasicDataSelect v-model="query.customerId" resource="customers" /></el-form-item>
        <el-form-item v-if="!inventory && config.suppliers && query.statType !== 'sale'" label="供应商"><BasicDataSelect v-model="query.supplierId" resource="suppliers" /></el-form-item>
        <el-form-item v-if="scenario === 'trend' || scenario === 'category-share'" label="业务类型">
          <el-select v-model="query.statType"><el-option v-if="scenario === 'trend'" :value="undefined" label="采购及销售" /><el-option value="sale" label="销售" /><el-option value="purchase" label="采购" /><el-option v-if="scenario === 'category-share'" value="inventory" label="当前库存" /></el-select>
        </el-form-item>
        <el-form-item v-if="!inventory" label="趋势粒度"><el-select v-model="query.groupBy"><el-option value="day" label="按日" /><el-option value="month" label="按月" /></el-select></el-form-item>
        <el-form-item v-if="scenario === 'inventory-structure'" label="统计维度"><el-select v-model="query.dimension"><el-option value="product" label="商品" /><el-option value="category" label="分类" /><el-option value="warehouse" label="仓库" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="load">查询</el-button><el-button @click="reset">重置</el-button><el-button :key="scenario" v-permission="`statistics:${scenario}:export`" :loading="exporting" @click="exportData">导出 Excel</el-button></el-form-item>
      </el-form>
      <p v-if="inventory" class="hint">库存价值按批次剩余基础数量与基础单位进价计算，库存结构按当前快照统计。</p>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    </el-card>
    <div v-loading="loading" class="content">
      <div class="metrics"><el-card v-for="metric in metrics" :key="metric.key" shadow="never"><div class="metric-label">{{ metric.name }}</div><div class="metric-value">{{ formatMetric(metric) }}</div></el-card></div>
      <el-card shadow="never">
        <div slot="header">{{ config.title }}明细 <span v-if="!config.timeSeries" class="hint">金额单位：元 · 数量按商品基础单位展示</span></div>
        <ChartSwitcher :key="scenario" :rows="result.rows || []" :time-series="!!config.timeSeries" :columns="columns" :initial-type="config.timeSeries ? 'line' : scenario === 'category-share' ? 'pie' : 'bar'" />
      </el-card>
      <el-card v-if="!config.timeSeries && !inventory && result.trendRows && result.trendRows.length" shadow="never">
        <div slot="header">按{{ query.groupBy === 'month' ? '月' : '日' }}进销趋势</div>
        <ChartSwitcher :key="`${scenario}-trend`" :rows="result.trendRows" time-series initial-type="line" :columns="trendColumns" />
      </el-card>
      <template v-if="scenario === 'dashboard'">
        <el-card v-for="section in dashboardSections" :key="section.key" shadow="never"><div slot="header">{{ section.title }}</div><ChartSwitcher :rows="result[section.key] || []" :columns="section.columns" /></el-card>
      </template>
    </div>
  </div>
</template>
<script>
import BasicDataSelect from '@/components/BasicDataSelect'
import ChartSwitcher from '@/components/ChartSwitcher'
import { getStatistics, exportStatistics } from '@/api/statistics/statistics'
import { downloadFile } from '@/utils/download'
import { scenarios, isInventory, defaultStatisticsQuery, statisticsParams, validateStatisticsQuery, statisticsColumns } from './scenarios'

const emptyResult = () => ({ rows: [], metrics: [], trendRows: [], saleTop: [], customerTop: [], supplierTop: [] })
export default {
  name: 'ScenarioPage',
  components: { BasicDataSelect, ChartSwitcher },
  props: { scenario: { type: String, required: true } },
  data: () => ({ query: {}, result: emptyResult(), loading: false, exporting: false, error: '', requestId: 0 }),
  computed: {
    config() { return scenarios[this.scenario] },
    inventory() { return isInventory(this.scenario, this.query) },
    columns() { return statisticsColumns(this.scenario, this.inventory, this.config.timeSeries) },
    trendColumns() { return statisticsColumns('trend', false, true) },
    metrics() { return this.result.metrics && this.result.metrics.length ? this.result.metrics : [{ key: 'amount', name: this.inventory ? '库存价值预估' : '金额合计', value: this.result.totalAmount || 0 }, { key: 'qty', name: '基础数量合计', value: this.result.totalQty || 0 }] },
    dashboardSections() { return [{ key: 'saleTop', title: '商品销售 Top 5', columns: statisticsColumns('sale-product') }, { key: 'customerTop', title: '客户销售 Top 5', columns: statisticsColumns('customer-rank') }, { key: 'supplierTop', title: '供应商采购 Top 5', columns: statisticsColumns('supplier-rank') }] }
  },
  watch: { scenario() { this.result = emptyResult(); this.reset() } },
  created() { this.query = defaultStatisticsQuery(this.scenario, this.$route && this.$route.query.groupBy === 'month' ? 'month' : 'day'); this.load() },
  beforeDestroy() { this.requestId++ },
  methods: {
    async load() {
      const id = ++this.requestId
      this.error = validateStatisticsQuery(this.scenario, this.query)
      if (this.error) { this.loading = false; return }
      this.loading = true
      try {
        const result = await getStatistics(this.scenario, statisticsParams(this.scenario, this.query))
        if (id === this.requestId) this.result = { ...emptyResult(), ...result }
      } catch (e) { if (id === this.requestId) this.error = e.message || '统计加载失败' }
      finally { if (id === this.requestId) this.loading = false }
    },
    reset() { this.query = defaultStatisticsQuery(this.scenario); return this.load() },
    formatMetric(metric) { const value = Number(metric.value || 0); return /amount/i.test(metric.key) ? `${value.toFixed(2)} 元` : `${Number(value.toFixed(4))}` },
    async exportData() {
      if (this.exporting) return
      this.error = validateStatisticsQuery(this.scenario, this.query); if (this.error) return
      this.exporting = true
      try { await downloadFile(await exportStatistics(this.scenario, statisticsParams(this.scenario, this.query)), `${this.config.title}.xlsx`) }
      catch (e) { this.error = e.message || '统计导出失败' }
      finally { this.exporting = false }
    }
  }
}
</script>
<style scoped>
.page, .content { display: flex; flex-direction: column; gap: 16px; }
.heading { display: flex; align-items: center; gap: 16px; }
.metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
.metric-label { color: #606266; font-size: 14px; }
.metric-value { font-size: 24px; margin-top: 10px; color: #1f2937; }
.hint { color: #606266; font-size: 12px; }
</style>
