<template>
  <div class="chart-switcher">
    <div class="chart-toolbar">
      <el-radio-group v-model="chartType" size="small"><el-radio-button v-for="item in chartTypes" :key="item.value" :label="item.value">{{ item.label }}</el-radio-button></el-radio-group>
      <div class="chart-options">
        <el-radio-group v-model="metric" size="small"><el-radio-button label="amount">金额</el-radio-button><el-radio-button label="qty">基础数量</el-radio-button></el-radio-group>
        <el-select v-if="!timeSeries && chartType === 'bar'" v-model="top" size="small" class="top-select"><el-option :value="10" label="Top 10" /><el-option :value="20" label="Top 20" /><el-option :value="0" label="全部" /></el-select>
      </div>
    </div>
    <p v-if="quantityUnit" class="hint">基础单位：{{ quantityUnit }}<span v-if="quantityUnit === '多种基础单位'">，请结合各行基础单位查看数量</span></p>
    <div v-show="chartType !== 'table'" ref="chart" class="chart" role="img" :aria-label="`${metric === 'amount' ? '金额' : '基础数量'}统计图`" />
    <p v-if="!rows.length && chartType !== 'table'" class="empty">暂无统计数据</p>
    <el-table v-if="chartType === 'table'" :data="tableRows" border>
      <el-table-column v-for="column in tableColumns" :key="column.prop" :prop="column.prop" :label="column.label" :min-width="column.width || 130">
        <template slot-scope="{ row }">{{ cellValue(row, column) }}</template>
      </el-table-column>
    </el-table>
    <p v-if="!timeSeries && chartType === 'bar'" class="hint">Top 仅限制柱状图，饼图、表格和 Excel 包含全部查询结果。</p>
  </div>
</template>
<script>
import * as echarts from 'echarts'
import { chartOption, statisticsColumns } from '@/views/statistics/scenarios'

export default {
  name: 'ChartSwitcher',
  props: { rows: { type: Array, default: () => [] }, timeSeries: Boolean, columns: { type: Array, default: () => [] }, initialType: { type: String, default: 'bar' } },
  data() { return { chartType: this.initialType, metric: 'amount', top: 10 } },
  computed: {
    chartTypes() { return [{ value: 'pie', label: '饼图' }, { value: 'bar', label: '柱状图' }, ...(this.timeSeries ? [{ value: 'line', label: '折线图' }] : []), { value: 'table', label: '表格' }] },
    tableRows() { return this.rows },
    tableColumns() { return this.columns.length ? this.columns : statisticsColumns('', false, this.timeSeries) },
    quantityUnit() { const units = [...new Set(this.rows.map(row => row.unitName).filter(Boolean))]; return units.length > 1 || units.includes('多种基础单位') ? '多种基础单位' : units[0] || '' }
  },
  watch: {
    rows: { deep: true, handler() { this.draw() } },
    chartType() { this.$nextTick(() => { this.draw(); this.resize() }) },
    metric() { this.draw() }, top() { this.draw() },
    timeSeries(value) { if (!value && this.chartType === 'line') this.chartType = 'bar'; this.draw() }
  },
  mounted() {
    this.chartInstance = echarts.init(this.$refs.chart)
    this.draw()
    window.addEventListener('resize', this.resize)
    if (typeof ResizeObserver !== 'undefined') { this.resizeObserver = new ResizeObserver(this.resize); this.resizeObserver.observe(this.$el) }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resize)
    if (this.resizeObserver) this.resizeObserver.disconnect()
    if (this.chartInstance) { this.chartInstance.dispose(); this.chartInstance = null }
  },
  methods: {
    resize() { if (this.chartInstance && this.chartType !== 'table') this.chartInstance.resize() },
    draw() { if (this.chartInstance && this.chartType !== 'table') this.chartInstance.setOption(chartOption(this.rows, this.chartType, this.metric, this.top, this.timeSeries), true) },
    cellValue(row, column) {
      const value = row[column.prop]
      if (value === null || value === undefined || value === '') return '—'
      if (column.money) return Number(value).toFixed(2)
      if (column.rate) return `${Number(value).toFixed(2)}%`
      return value
    }
  }
}
</script>
<style scoped>
.chart-toolbar, .chart-options { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.chart-toolbar { justify-content: space-between; margin-bottom: 12px; }
.top-select { width: 110px; }
.chart { width: 100%; height: 400px; }
.hint, .empty { color: #606266; font-size: 12px; }
.empty { text-align: center; }
</style>
