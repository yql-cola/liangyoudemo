<template>
  <el-card shadow="never">
    <h2>{{ title }}</h2>
    <p>该基础资料模块的后端 Controller 尚未实现，一期仅预留菜单入口。</p>
  </el-card>
</template>

<script>
export default {
  name: 'BasicPlaceholder',
  computed: {
    title() {
      return this.$route.meta && this.$route.meta.title ? this.$route.meta.title : '基础资料'
    }
  }
}
</script>
