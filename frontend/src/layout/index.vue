<template>
  <div class="app-wrapper">
    <Sidebar class="sidebar-container" />
    <div class="main-container">
      <Navbar />
      <AppMain />
    </div>
  </div>
</template>

<script>
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import AppMain from './components/AppMain.vue'

export default {
  name: 'Layout',
  components: { Sidebar, Navbar, AppMain }
}
</script>

<style lang="scss" scoped>
.app-wrapper {
  display: flex;
  min-height: 100vh;
  background: #f3f5f8;
}

.sidebar-container {
  width: 220px;
  flex: 0 0 220px;
}

.main-container {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
