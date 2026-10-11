<template>
  <StatisticPage :loader="dailyInboundOutbound" daily />
</template>

<script>
import StatisticPage from './StatisticPage.vue'
import { dailyInboundOutbound } from '@/api/statistics/statistics'

export default {
  name: 'StatisticsDaily',
  components: { StatisticPage },
  methods: { dailyInboundOutbound }
}
</script>
