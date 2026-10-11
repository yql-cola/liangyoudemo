<template>
  <StatisticPage :loader="amountBySupplier" />
</template>

<script>
import StatisticPage from './StatisticPage.vue'
import { amountBySupplier } from '@/api/statistics/statistics'

export default {
  name: 'StatisticsSupplier',
  components: { StatisticPage },
  methods: { amountBySupplier }
}
</script>
