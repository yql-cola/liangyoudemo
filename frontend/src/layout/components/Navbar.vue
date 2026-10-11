<template>
  <header class="navbar">
    <div class="title">{{ title }}</div>
    <el-dropdown trigger="click" @command="handleCommand">
      <span class="user-menu">
        {{ realName || username || '用户' }}
        <i class="el-icon-arrow-down el-icon--right" />
      </span>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item command="logout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </header>
</template>

<script>
export default {
  name: 'Navbar',
  computed: {
    title() {
      return this.$route.meta && this.$route.meta.title ? this.$route.meta.title : '后台管理'
    },
    username() {
      return this.$store.state.user.username
    },
    realName() {
      return this.$store.state.user.realName
    }
  },
  methods: {
    async handleCommand(command) {
      if (command === 'logout') {
        await this.$store.dispatch('user/logout')
        this.$router.push('/login')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e6e9ef;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.user-menu {
  cursor: pointer;
  color: #334155;
}
</style>
