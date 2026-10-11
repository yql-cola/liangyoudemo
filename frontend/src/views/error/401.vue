<template>
  <div class="error-page">
    <h1>401</h1>
    <p>没有权限访问该页面</p>
    <el-button type="primary" @click="$router.replace('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  padding: 80px 24px;
  text-align: center;
}
</style>
