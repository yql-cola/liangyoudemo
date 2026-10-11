<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="仓库ID"><el-input-number v-model="query.warehouseId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="统计类型"><el-input v-model="query.statType" placeholder="purchase / sale" clearable /></el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="query.dateFrom" value-format="yyyy-MM-dd" type="date" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="query.dateTo" value-format="yyyy-MM-dd" type="date" /></el-form-item>
        <el-form-item label="开始月份"><el-input v-model="query.monthFrom" placeholder="2026-04" clearable /></el-form-item>
        <el-form-item label="结束月份"><el-input v-model="query.monthTo" placeholder="2026-06" clearable /></el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div ref="chart" class="chart" />
    </el-card>
    <el-card shadow="never">
      <el-table :data="list" border>
        <el-table-column v-if="daily" prop="statDate" label="统计日期" />
        <el-table-column v-if="daily" prop="statType" label="统计类型" />
        <el-table-column v-if="!daily" prop="key" label="维度" />
        <el-table-column prop="totalQty" label="总数量" />
        <el-table-column prop="totalAmount" label="总金额" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'StatisticPage',
  props: {
    loader: {
      type: Function,
      required: true
    },
    daily: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return { query: {}, list: [] }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      this.list = await this.loader(this.query)
      this.draw()
    },
    reset() {
      this.query = {}
      this.load()
    },
    draw() {
      const chart = echarts.init(this.$refs.chart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: this.list.map(item => this.daily ? item.statDate : item.key) },
        yAxis: { type: 'value' },
        series: [
          { name: '总金额', type: 'bar', data: this.list.map(item => Number(item.totalAmount || 0)) },
          { name: '总数量', type: 'line', data: this.list.map(item => Number(item.totalQty || 0)) }
        ]
      })
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.chart { height: 320px; }
</style>
