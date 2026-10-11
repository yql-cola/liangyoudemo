<template>
  <div class="dashboard page">
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="never">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card shadow="never" class="chart-card">
      <div slot="header">近 7 天进销趋势</div>
      <div ref="chart" class="chart" />
    </el-card>
    <el-card shadow="never">
      <div slot="header">快捷入口</div>
      <el-button type="primary" @click="$router.push('/purchase/in')">采购入库</el-button>
      <el-button type="success" @click="$router.push('/sale/out')">销售出库</el-button>
      <el-button @click="$router.push('/inventory/stock')">库存查询</el-button>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { dailyInboundOutbound } from '@/api/statistics/statistics'
import { listInventory } from '@/api/inventory/inventory'

export default {
  name: 'Dashboard',
  data() {
    return {
      stockTotal: 0,
      trend: []
    }
  },
  computed: {
    todayPurchase() {
      return this.sumToday('purchase')
    },
    todaySale() {
      return this.sumToday('sale')
    },
    cards() {
      return [
        { label: '今日采购金额', value: this.todayPurchase },
        { label: '今日销售金额', value: this.todaySale },
        { label: '当前库存 SKU 数', value: this.stockTotal },
        { label: '库存预警', value: '待后端接口补齐' }
      ]
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const now = new Date()
      const end = now.toISOString().slice(0, 10)
      const startDate = new Date(now.getTime() - 6 * 24 * 3600 * 1000)
      const start = startDate.toISOString().slice(0, 10)
      this.trend = await dailyInboundOutbound({ dateFrom: start, dateTo: end })
      const stock = await listInventory({ pageNum: 1, pageSize: 1 })
      this.stockTotal = stock.total || 0
      this.draw()
    },
    sumToday(type) {
      const today = new Date().toISOString().slice(0, 10)
      return this.trend
        .filter(item => item.statDate === today && item.statType === type)
        .reduce((sum, item) => sum + Number(item.totalAmount || 0), 0)
        .toFixed(2)
    },
    draw() {
      const chart = echarts.init(this.$refs.chart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: this.trend.map(item => item.statDate) },
        yAxis: { type: 'value' },
        series: [
          {
            name: '金额',
            type: 'line',
            data: this.trend.map(item => Number(item.totalAmount || 0))
          }
        ]
      })
    }
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.metric-label {
  color: #64748b;
}
.metric-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}
.chart {
  height: 320px;
}
</style>
