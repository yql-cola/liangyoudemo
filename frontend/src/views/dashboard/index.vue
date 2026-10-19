<template>
  <div class="page">
    <ScenarioPage v-if="canViewDashboard" scenario="dashboard" />
    <el-card v-else shadow="never"><p>欢迎使用粮油进销存系统，请从菜单选择有权限的业务模块。</p></el-card>
    <el-card shadow="never">
      <div slot="header">快捷入口</div>
      <el-button v-permission="'purchase:order:list'" type="primary" @click="$router.push('/purchase/in')">采购入库</el-button>
      <el-button v-permission="'sale:order:list'" type="success" @click="$router.push('/sale/out')">销售出库</el-button>
      <el-button v-permission="'inventory:stock:list'" @click="$router.push('/inventory/stock')">库存查询</el-button>
    </el-card>
  </div>
</template>
<script>
import ScenarioPage from '@/views/statistics/ScenarioPage'
import { hasPermission } from '@/utils/permission'
export default {
  name: 'Dashboard',
  components: { ScenarioPage },
  computed: { canViewDashboard() { return hasPermission(this.$store.state.user.permissions, 'statistics:dashboard:view') } }
}
</script>
<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
</style>
