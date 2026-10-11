<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="query.roleCode" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable>
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-permission="'system:role:create'" type="primary" @click="open()">新增</el-button>
        <RightToolbar @queryTable="load" />
      </div>
      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="{ row }"><DictTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="260">
          <template slot-scope="{ row }">
            <el-button v-permission="'system:role:update'" type="text" @click="open(row)">编辑</el-button>
            <el-button v-permission="'system:role:grant'" type="text" @click="openPerm(row)">分配权限</el-button>
            <el-button v-permission="'system:role:delete'" type="text" class="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog :title="form.id ? '编辑角色' : '新增角色'" :visible.sync="dialogVisible" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色编码"><el-input v-model="form.roleCode" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option :value="1" label="启用" /><el-option :value="0" label="停用" /></el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </span>
    </el-dialog>
    <el-dialog title="分配权限" :visible.sync="permVisible" width="520px">
      <el-input v-model="permissionIds" type="textarea" :rows="4" placeholder="请输入权限 ID，多个用英文逗号分隔" />
      <span slot="footer">
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listRole, addRole, updateRole, deleteRole, assignRolePermissions } from '@/api/system/role'
import RightToolbar from '@/components/RightToolbar'
import DictTag from '@/components/DictTag'

export default {
  name: 'SystemRole',
  components: { RightToolbar, DictTag },
  data() {
    return {
      query: {},
      list: [],
      dialogVisible: false,
      permVisible: false,
      form: {},
      currentRole: null,
      permissionIds: ''
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.list = await listRole(this.query)
    },
    reset() {
      this.query = {}
      this.load()
    },
    open(row) {
      this.form = row ? { ...row } : { status: 1 }
      this.dialogVisible = true
    },
    async save() {
      if (this.form.id) await updateRole(this.form.id, this.form)
      else await addRole(this.form)
      this.dialogVisible = false
      this.load()
    },
    async remove(row) {
      await this.$confirm(`确认删除角色 ${row.roleName}？`, '提示')
      await deleteRole(row.id)
      this.load()
    },
    openPerm(row) {
      this.currentRole = row
      this.permissionIds = ''
      this.permVisible = true
    },
    async savePerm() {
      const permissionIds = this.permissionIds.split(',').map(item => Number(item.trim())).filter(Boolean)
      await assignRolePermissions(this.currentRole.id, { permissionIds })
      this.permVisible = false
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
.danger { color: #f56c6c; }
</style>
