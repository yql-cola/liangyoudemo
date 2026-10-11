<template>
  <StatisticPage :loader="monthlyInboundOutbound" />
</template>

<script>
import StatisticPage from './StatisticPage.vue'
import { monthlyInboundOutbound } from '@/api/statistics/statistics'

export default {
  name: 'StatisticsMonthly',
  components: { StatisticPage },
  methods: { monthlyInboundOutbound }
}
</script>
