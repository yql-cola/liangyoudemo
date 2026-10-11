<template>
  <el-card shadow="never">
    <div slot="header">
      <span>菜单权限</span>
      <el-button size="mini" icon="el-icon-refresh" class="pull-right" @click="load">刷新</el-button>
    </div>
    <el-empty v-if="tree.length === 0" description="暂无可见菜单" />
    <el-tree
      v-else
      :data="tree"
      node-key="id"
      default-expand-all
      :props="{ label: 'name', children: 'children' }"
    />
  </el-card>
</template>

<script>
import { getMenuTree } from '@/api/system/menu'

export default {
  name: 'SystemMenu',
  data() {
    return {
      tree: []
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.tree = await getMenuTree()
    }
  }
}
</script>

<style scoped>
.pull-right {
  float: right;
}
</style>
