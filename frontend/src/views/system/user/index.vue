<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="用户名"><el-input v-model="query.username" clearable /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="query.realName" clearable /></el-form-item>
        <el-form-item label="仓库ID"><el-input-number v-model="query.warehouseId" :min="1" controls-position="right" /></el-form-item>
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
        <el-button v-permission="'system:user:create'" type="primary" @click="open()">新增</el-button>
        <RightToolbar @queryTable="load" />
      </div>
      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="warehouseId" label="仓库ID" />
        <el-table-column prop="status" label="状态">
          <template slot-scope="{ row }"><DictTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template slot-scope="{ row }">
            <el-button v-permission="'system:user:update'" type="text" @click="open(row)">编辑</el-button>
            <el-button v-permission="'system:user:grant-role'" type="text" @click="openRoles(row)">分配角色</el-button>
            <el-button v-permission="'system:user:grant-permission'" type="text" @click="openPerm(row)">单独授权</el-button>
            <el-button v-permission="'system:user:delete'" type="text" class="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />
    </el-card>
    <el-dialog :title="form.id ? '编辑用户' : '新增用户'" :visible.sync="dialogVisible" width="620px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名" v-if="!form.id"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码" v-if="!form.id"><el-input v-model="form.password" show-password /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="仓库ID"><el-input-number v-model="form.warehouseId" :min="1" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option :value="1" label="启用" /><el-option :value="0" label="停用" /></el-select></el-form-item>
        <el-form-item label="超级管理员"><el-switch v-model="form.isSuperAdmin" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </span>
    </el-dialog>
    <el-dialog title="分配角色" :visible.sync="roleVisible" width="520px">
      <el-input v-model="roleIds" placeholder="请输入角色 ID，多个用英文逗号分隔" />
      <span slot="footer">
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoles">保存</el-button>
      </span>
    </el-dialog>
    <el-dialog title="用户单独授权" :visible.sync="permVisible" width="620px">
      <el-form label-width="130px">
        <el-form-item label="授予权限ID"><el-input v-model="grantPermissionIds" placeholder="多个用英文逗号分隔" /></el-form-item>
        <el-form-item label="回收权限ID"><el-input v-model="revokePermissionIds" placeholder="多个用英文逗号分隔" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listUser, addUser, updateUser, deleteUser, assignUserRoles, assignUserPermissions } from '@/api/system/user'
import Pagination from '@/components/Pagination'
import RightToolbar from '@/components/RightToolbar'
import DictTag from '@/components/DictTag'

export default {
  name: 'SystemUser',
  components: { Pagination, RightToolbar, DictTag },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10 },
      list: [],
      total: 0,
      dialogVisible: false,
      roleVisible: false,
      permVisible: false,
      form: {},
      currentUser: null,
      roleIds: '',
      grantPermissionIds: '',
      revokePermissionIds: ''
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      const data = await listUser(this.query)
      this.list = data.list || []
      this.total = data.total || 0
    },
    reset() {
      this.query = { pageNum: 1, pageSize: 10 }
      this.load()
    },
    open(row) {
      this.form = row ? { ...row } : { status: 1, isSuperAdmin: 0, warehouseId: 1 }
      this.dialogVisible = true
    },
    async save() {
      if (this.form.id) await updateUser(this.form.id, this.form)
      else await addUser(this.form)
      this.dialogVisible = false
      this.load()
    },
    async remove(row) {
      await this.$confirm(`确认删除用户 ${row.username}？`, '提示')
      await deleteUser(row.id)
      this.load()
    },
    openRoles(row) {
      this.currentUser = row
      this.roleIds = (row.roleIds || []).join(',')
      this.roleVisible = true
    },
    async saveRoles() {
      await assignUserRoles(this.currentUser.id, { roleIds: this.parseIds(this.roleIds) })
      this.roleVisible = false
      this.load()
    },
    openPerm(row) {
      this.currentUser = row
      this.grantPermissionIds = ''
      this.revokePermissionIds = ''
      this.permVisible = true
    },
    async savePerm() {
      await assignUserPermissions(this.currentUser.id, {
        grantPermissionIds: this.parseIds(this.grantPermissionIds),
        revokePermissionIds: this.parseIds(this.revokePermissionIds)
      })
      this.permVisible = false
    },
    parseIds(value) {
      return String(value || '').split(',').map(item => Number(item.trim())).filter(Boolean)
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
.danger { color: #f56c6c; }
</style>
