<template>
  <main class="app-main">
    <router-view />
  </main>
</template>

<script>
export default {
  name: 'AppMain'
}
</script>

<style lang="scss" scoped>
.app-main {
  flex: 1;
  min-width: 0;
  padding: 16px;
  overflow: auto;
}
</style>
