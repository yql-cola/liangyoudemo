<template>
  <StatisticPage :loader="amountByCustomer" />
</template>

<script>
import StatisticPage from './StatisticPage.vue'
import { amountByCustomer } from '@/api/statistics/statistics'

export default {
  name: 'StatisticsCustomer',
  components: { StatisticPage },
  methods: { amountByCustomer }
}
</script>
