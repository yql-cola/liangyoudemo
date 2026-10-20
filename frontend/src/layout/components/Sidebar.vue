<template>
  <aside class="sidebar">
    <div class="brand">粮油进销存</div>
    <el-menu
      :default-active="$route.path"
      router
      background-color="#263445"
      text-color="#cfd7e6"
      active-text-color="#ffffff"
    >
      <template v-for="route in menuRoutes">
        <el-submenu
          v-if="visibleChildren(route).length > 1"
          :key="route.path"
          :index="route.path"
        >
          <template slot="title">
            <i :class="route.meta && route.meta.icon" />
            <span>{{ route.meta && route.meta.title }}</span>
          </template>
          <el-menu-item
            v-for="child in visibleChildren(route)"
            :key="resolvePath(route.path, child.path)"
            :index="resolvePath(route.path, child.path)"
          >
            {{ child.meta && child.meta.title }}
          </el-menu-item>
        </el-submenu>
        <el-menu-item
          v-else-if="visibleChildren(route).length === 1"
          :key="route.path"
          :index="resolvePath(route.path, visibleChildren(route)[0].path)"
        >
          <i :class="route.meta && route.meta.icon" />
          <span slot="title">{{ visibleChildren(route)[0].meta && visibleChildren(route)[0].meta.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </aside>
</template>

<script>
import { constantRoutes } from '@/router'
import { hasPermission } from '@/utils/permission'

export default {
  name: 'Sidebar',
  computed: {
    permissions() {
      return this.$store.state.user.permissions
    },
    menuRoutes() {
      return constantRoutes.filter(route => !route.hidden && route.children)
    }
  },
  methods: {
    visibleChildren(route) {
      return (route.children || []).filter(child => {
        const required = child.meta && child.meta.permission
        return !child.hidden && hasPermission(this.permissions, required)
      })
    },
    resolvePath(parentPath, childPath) {
      if (parentPath === '/') return `/${childPath}`
      return `${parentPath}/${childPath}`.replace(/\/+/g, '/')
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #263445;
}

.brand {
  flex-shrink: 0;
  height: 56px;
  line-height: 56px;
  color: #fff;
  font-weight: 600;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.el-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: 0;
}
</style>
