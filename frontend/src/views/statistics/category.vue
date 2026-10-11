<template>
  <StatisticPage :loader="amountByCategory" />
</template>

<script>
import StatisticPage from './StatisticPage.vue'
import { amountByCategory } from '@/api/statistics/statistics'

export default {
  name: 'StatisticsCategory',
  components: { StatisticPage },
  methods: { amountByCategory }
}
</script>
