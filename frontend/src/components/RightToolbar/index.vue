<template>
  <div class="right-toolbar">
    <el-tooltip content="刷新" placement="top">
      <el-button icon="el-icon-refresh" circle @click="$emit('queryTable')" />
    </el-tooltip>
  </div>
</template>

<script>
export default {
  name: 'RightToolbar'
}
</script>

<style scoped>
.right-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
