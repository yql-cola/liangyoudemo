<template>
  <div class="page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="角色名称"><el-input v-model="query.roleName" clearable /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="query.roleCode" clearable /></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable><el-option :value="1" label="启用" /><el-option :value="0" label="停用" /></el-select></el-form-item>
        <el-button type="primary" :loading="listLoading" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-permission="'system:role:create'" type="primary" @click="open()">新增</el-button>
        <RightToolbar @queryTable="load" />
      </div>
      <el-table v-loading="listLoading" :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="status" label="状态" width="100"><template slot-scope="{ row }"><DictTag :value="row.status" /></template></el-table-column>
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
    <el-dialog :title="form.id ? '编辑角色' : '新增角色'" :visible.sync="dialogVisible" width="620px">
      <el-form ref="roleForm" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode"><el-input v-model="form.roleCode" /></el-form-item>
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="状态" prop="status"><el-select v-model="form.status"><el-option :value="1" label="启用" /><el-option :value="0" label="停用" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        <el-form-item v-if="canGrant" label="角色权限">
          <div v-loading="dialogLoading" class="permission-box">
            <el-empty v-if="!dialogLoading && permissionTree.length === 0" description="暂无可分配权限" />
            <el-tree v-else ref="roleTree" :data="permissionTree" node-key="id" show-checkbox check-strictly default-expand-all :props="treeProps">
              <span slot-scope="{ data }">{{ data.permissionName }} <small>({{ typeLabel(data.permissionType) }})</small> <el-tag v-if="data.status !== 1" size="mini" type="info">已停用</el-tag></span>
            </el-tree>
          </div>
          <el-alert v-if="dialogDisabled.length" type="warning" :closable="false" :title="disabledNotice(dialogDisabled)" />
          <el-alert v-if="dialogLoadFailed" type="error" :closable="false" title="权限回显失败，请重试后保存" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="dialogLoading || dialogLoadFailed" @click="save">保存</el-button>
      </span>
    </el-dialog>
    <el-dialog title="分配权限" :visible.sync="permVisible" width="620px">
      <div v-loading="permLoading" class="permission-box">
        <el-empty v-if="!permLoading && permissionTree.length === 0" description="暂无可分配权限" />
        <el-tree v-else ref="permTree" :data="permissionTree" node-key="id" show-checkbox check-strictly default-expand-all :props="treeProps">
          <span slot-scope="{ data }">{{ data.permissionName }} <small>({{ typeLabel(data.permissionType) }})</small> <el-tag v-if="data.status !== 1" size="mini" type="info">已停用</el-tag></span>
        </el-tree>
      </div>
      <el-alert v-if="permDisabled.length" type="warning" :closable="false" :title="disabledNotice(permDisabled)" />
      <el-alert v-if="permLoadFailed" type="error" :closable="false" title="权限回显失败，请重试后保存" />
      <span slot="footer">
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="permLoading || permLoadFailed" @click="savePerm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { listRole, addRole, updateRole, deleteRole, assignRolePermissions, getAssignablePermissions, getRolePermissionIds } from '@/api/system/role'
import { hasPermission } from '@/utils/permission'
import RightToolbar from '@/components/RightToolbar'
import DictTag from '@/components/DictTag'

const EFFECT_NOTICE = '权限变更对已登录用户重新登录或 Token 到期后生效'

export default {
  name: 'SystemRole',
  components: { RightToolbar, DictTag },
  data() {
    return {
      query: {}, list: [], listLoading: false,
      dialogVisible: false, permVisible: false, dialogLoading: false, permLoading: false,
      dialogLoadFailed: false, permLoadFailed: false, saving: false,
      form: { status: 1 }, currentRole: null, permissionTree: [],
      dialogDisabled: [], permDisabled: [],
      treeProps: { label: 'permissionName', children: 'children', disabled: 'disabled' },
      rules: {
        roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    canGrant() { return hasPermission(this.$store.state.user.permissions, 'system:role:grant') }
  },
  created() { this.load() },
  methods: {
    async load() {
      this.listLoading = true
      try { this.list = await listRole(this.query) } finally { this.listLoading = false }
    },
    reset() { this.query = {}; this.load() },
    typeLabel(type) { return { 1: '菜单/页面', 2: '按钮', 3: '接口' }[type] || '权限' },
    disabledNotice(nodes) {
      return '原授权中有已停用权限：' + nodes.map(item => item.permissionName).join('、') + '；保存时将移除这些关联'
    },
    markDisabled(nodes) {
      return nodes.map(node => ({ ...node, disabled: node.status !== 1, children: this.markDisabled(node.children || []) }))
    },
    flatten(nodes, result = {}) {
      nodes.forEach(node => { result[node.id] = node; this.flatten(node.children || [], result) })
      return result
    },
    async loadSelection(roleId, treeRef, loadingKey, failureKey, disabledKey) {
      this[loadingKey] = true
      this[failureKey] = false
      this[disabledKey] = []
      try {
        const [tree, assigned] = await Promise.all([getAssignablePermissions(), roleId ? getRolePermissionIds(roleId) : Promise.resolve([])])
        this.permissionTree = this.markDisabled(tree)
        const byId = this.flatten(this.permissionTree)
        this[disabledKey] = assigned.filter(id => !byId[id] || byId[id].status !== 1)
          .map(id => byId[id] || { permissionName: 'ID ' + id })
        await this.$nextTick()
        // Strict checking keeps parent grouping separate from granted IDs.
        if (this.$refs[treeRef]) this.$refs[treeRef].setCheckedKeys(assigned.filter(id => byId[id] && byId[id].status === 1))
      } catch (error) {
        this[failureKey] = true
      } finally { this[loadingKey] = false }
    },
    open(row) {
      this.form = row ? { ...row } : { status: 1 }
      this.dialogVisible = true
      this.dialogLoadFailed = false
      this.permissionTree = []
      if (this.canGrant) this.loadSelection(row && row.id, 'roleTree', 'dialogLoading', 'dialogLoadFailed', 'dialogDisabled')
    },
    checkedIds(treeRef) { return this.$refs[treeRef] ? this.$refs[treeRef].getCheckedKeys() : [] },
    async save() {
      if (this.saving || this.dialogLoading || this.dialogLoadFailed) return
      try { await this.$refs.roleForm.validate() } catch (error) { return }
      const data = { roleCode: this.form.roleCode, roleName: this.form.roleName, status: this.form.status, remark: this.form.remark }
      if (this.canGrant) data.permissionIds = this.checkedIds('roleTree')
      this.saving = true
      try {
        if (this.form.id) await updateRole(this.form.id, data)
        else await addRole(data)
        this.dialogVisible = false
        this.$message.success(this.canGrant ? EFFECT_NOTICE : '保存成功')
        await this.load()
      } finally { this.saving = false }
    },
    async remove(row) {
      await this.$confirm('确认删除角色 ' + row.roleName + '？', '提示')
      await deleteRole(row.id)
      await this.load()
    },
    openPerm(row) {
      this.currentRole = row
      this.permVisible = true
      this.permLoadFailed = false
      this.permissionTree = []
      this.loadSelection(row.id, 'permTree', 'permLoading', 'permLoadFailed', 'permDisabled')
    },
    async savePerm() {
      if (this.saving || this.permLoading || this.permLoadFailed) return
      this.saving = true
      try {
        await assignRolePermissions(this.currentRole.id, { permissionIds: this.checkedIds('permTree') })
        this.permVisible = false
        this.$message.success(EFFECT_NOTICE)
        await this.load()
      } finally { this.saving = false }
    }
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
.danger { color: #f56c6c; }
.permission-box { max-height: 320px; overflow: auto; border: 1px solid #ebeef5; border-radius: 4px; padding: 8px 12px; }
.permission-box small { color: #909399; }
</style>
