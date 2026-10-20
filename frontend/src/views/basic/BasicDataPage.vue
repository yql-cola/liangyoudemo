<template>
  <div class="basic-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="query" @submit.native.prevent="search">
        <el-form-item label="关键词"><el-input v-model.trim="query.keyword" clearable placeholder="编码、名称或联系人" @clear="search" @keyup.enter.native="search" /></el-form-item>
        <el-form-item v-for="filter in schema.filters" :key="filter.key" :label="filter.label">
          <BasicDataSelect v-model="query[filter.key]" :resource="filter.resource" @change="search" />
        </el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部" @change="search"><el-option :value="1" label="启用" /><el-option :value="0" label="停用" /></el-select></el-form-item>
        <el-form-item><el-button v-permission="permission('list')" type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-actions">
          <el-button v-permission="permission('create')" type="primary" icon="el-icon-plus" @click="open()">新增{{ schema.title }}</el-button>
          <el-button v-if="canImport" icon="el-icon-upload2" @click="openImport">批量导入</el-button>
          <el-button v-if="canImport" icon="el-icon-document" :loading="templateLoading" @click="downloadTemplate">下载导入模板</el-button>
          <el-button v-permission="permission('export')" icon="el-icon-download" :loading="exporting" @click="exportData">{{ selectedIds.length ? '导出所选' : '导出 Excel' }}</el-button>
        </div>
        <el-button v-permission="permission('list')" class="refresh-button" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
      <div v-if="selectedIds.length" class="selection-bar">
        <div class="selection-copy"><i class="el-icon-circle-check" /><strong>已选择 {{ selectedIds.length }} 条{{ schema.title }}资料</strong><span>导出时仅包含所选记录</span></div>
        <el-button type="text" class="selection-clear" @click="clearSelection">清空选择</el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="error-banner" />
      <el-table ref="dataTable" v-loading="loading" :data="list" row-key="id" border :default-expand-all="schema.tree" :tree-props="{ children: 'children' }" empty-text="暂无资料" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" reserve-selection />
        <el-table-column v-for="column in schema.columns" :key="column[0]" :prop="column[0]" :label="column[1]" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="85"><template slot-scope="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" :width="schema.tree ? 410 : 290" fixed="right">
          <template slot-scope="{ row }">
            <el-button v-permission="permission('view')" type="text" @click="open(row, 'view')">详情</el-button>
            <el-button v-if="canEdit" type="text" @click="open(row)">编辑</el-button>
            <template v-if="schema.tree">
              <el-button v-permission="permission('create')" type="text" @click="open(null, 'edit', row.parentId || 0)">新增同级</el-button>
              <el-button v-permission="permission('create')" type="text" @click="open(null, 'edit', row.id)">新增子级</el-button>
            </template>
            <el-button v-permission="permission('status')" type="text" :disabled="busyId === row.id" @click="changeStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button v-permission="permission('delete')" type="text" class="danger" :disabled="busyId === row.id" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-if="!schema.tree" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />
    </el-card>
    <el-dialog :title="`批量导入${schema.title}`" :visible.sync="importVisible" width="620px" :close-on-click-modal="false">
      <div class="import-intro"><i class="el-icon-info" /><span>请使用标准模板填写资料。系统会先校验全部数据，存在错误时整批不写入。</span></div>
      <label class="import-upload" :class="{ 'is-disabled': importing }">
        <input ref="importFileInput" type="file" accept=".xlsx" :disabled="importing" @change="chooseImportFile">
        <i class="el-icon-upload2" />
        <strong>{{ importFile ? importFile.name : '点击选择 XLSX 文件' }}</strong>
        <span>单个文件不超过 5 MB，最多 2000 行</span>
      </label>
      <div class="import-template">首次导入？<el-button type="text" :loading="templateLoading" @click="downloadTemplate">下载标准模板</el-button></div>
      <el-alert v-if="importMessage" class="import-result" :title="importMessage" :type="importResultType" :closable="false" show-icon />
      <el-table v-if="importErrors.length" class="import-errors" :data="importErrors" max-height="240" border>
        <el-table-column prop="rowNumber" label="Excel 行号" width="110" />
        <el-table-column prop="columnName" label="列名" width="130" />
        <el-table-column prop="message" label="错误原因" />
      </el-table>
      <span slot="footer"><el-button :disabled="importing" @click="importVisible = false">关闭</el-button><el-button type="primary" :loading="importing" :disabled="!importFile" @click="submitImport">开始导入</el-button></span>
    </el-dialog>
    <el-dialog :title="`${readOnly ? '查看' : form.id ? '编辑' : '新增'}${schema.title}`" :visible.sync="dialogVisible" width="760px" :close-on-click-modal="false" :before-close="closeDialog">
      <el-alert v-if="formError" :title="formError" type="error" show-icon :closable="false" class="error-banner" />
      <el-form ref="form" v-loading="detailLoading" :model="form" :rules="rules" :disabled="readOnly || saving || detailLoading" label-width="110px">
        <el-row :gutter="20">
          <el-col v-for="field in schema.fields" :key="field.key" :span="field.type === 'textarea' ? 24 : 12">
            <el-form-item :label="field.label" :prop="field.key">
              <BasicDataSelect v-if="field.resource" v-model="form[field.key]" :resource="field.resource" :selected-label="form[field.labelKey] || ''" :disabled="readOnly || saving || detailLoading" />
              <el-select v-else-if="field.type === 'parent'" v-model="form[field.key]" filterable placeholder="请选择上级分类">
                <el-option v-for="parent in parentOptions" :key="parent.id" :value="parent.id" :label="parent.name" :disabled="parent.status === 0" />
              </el-select>
              <template v-else-if="field.type === 'status'">
                <el-select v-model="form[field.key]" :disabled="readOnly || saving || detailLoading || !canChangeFormStatus"><el-option :value="1" label="启用" /><el-option :value="0" label="停用" /></el-select>
                <span v-if="form.id && !canChangeFormStatus && !readOnly" class="permission-hint">需要启停用权限才能修改状态</span>
              </template>
              <el-input-number v-else-if="field.type === 'number'" v-model="form[field.key]" :min="field.min" :precision="field.precision || 0" :controls="true" controls-position="right" />
              <el-input v-else v-model.trim="form[field.key]" :type="field.type === 'textarea' ? 'textarea' : 'text'" :maxlength="field.max" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <span slot="footer"><el-button :disabled="saving" @click="dialogVisible = false">{{ readOnly ? '关闭' : '取消' }}</el-button><el-button v-if="!readOnly" type="primary" :loading="saving" :disabled="detailLoading" @click="save">保存</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { createBasicApi, getCategoryTree } from '@/api/basic'
import { downloadFile } from '@/utils/download'
import BasicDataSelect from '@/components/BasicDataSelect'
import Pagination from '@/components/Pagination'
import { categoryParentOptions, filterCategoryTree } from './categoryTree'
import { hasPermission } from '@/utils/permission'

export default {
  name: 'BasicDataPage',
  components: { BasicDataSelect, Pagination },
  props: { schema: { type: Object, required: true } },
  data() {
    return { query: { pageNum: 1, pageSize: 10, keyword: '', status: undefined }, list: [], total: 0, loading: false,
      exporting: false, selectedIds: [], templateLoading: false, importVisible: false, importing: false, importFile: null, importErrors: [], importMessage: '', importResultType: 'success',
      error: '', formError: '', dialogVisible: false, readOnly: false, saving: false, detailLoading: false,
      form: {}, originalStatus: null, categoryTree: [], busyId: null, loadId: 0, detailId: 0 }
  },
  computed: {
    api() { return createBasicApi(this.schema.resource) },
    // Existing create grants may predate the dedicated import permission; importing also creates records.
    canImport() { return ['import', 'create'].some(action => hasPermission(this.$store.state.user.permissions, this.permission(action))) },
    // Editing loads full detail, so both operation permissions are required.
    canEdit() { return ['update', 'view'].every(action => hasPermission(this.$store.state.user.permissions, this.permission(action))) },
    canChangeFormStatus() { return !this.form.id || hasPermission(this.$store.state.user.permissions, this.permission('status')) },
    parentOptions() { return categoryParentOptions(this.categoryTree, this.form.id) },
    rules() {
      const rules = {}
      this.schema.fields.forEach(field => {
        const checks = []
        if (field.required) checks.push({ required: true, message: `请${field.resource || field.type === 'parent' || field.type === 'status' ? '选择' : '填写'}${field.label}`, trigger: 'change' })
        if (field.max) checks.push({ max: field.max, message: `${field.label}最多 ${field.max} 个字符`, trigger: 'blur' })
        if (field.type === 'number') checks.push({ type: 'number', min: field.min, message: `${field.label}不能小于 ${field.min}`, trigger: 'change' })
        if (field.key === 'toUnitId') checks.push({ validator: (rule, value, callback) => callback(value && value === this.form.fromUnitId ? new Error('源单位与目标单位不能相同') : undefined), trigger: 'change' })
        rules[field.key] = checks
      })
      return rules
    }
  },
  created() { this.load() },
  beforeDestroy() { this.loadId++; this.detailId++ },
  methods: {
    permission(action) { return `basic:${this.schema.key}:${action}` },
    filters() {
      const filters = { ...this.query }
      delete filters.pageNum
      delete filters.pageSize
      return Object.keys(filters).reduce((result, key) => {
        if (filters[key] !== undefined && filters[key] !== null && filters[key] !== '') result[key] = filters[key]
        return result
      }, {})
    },
    async load() {
      const id = ++this.loadId
      this.loading = true; this.error = ''
      try {
        const data = this.schema.tree ? await getCategoryTree(this.filters()) : await this.api.list({ ...this.query })
        if (id !== this.loadId) return
        this.list = this.schema.tree ? filterCategoryTree(data || [], this.filters()) : data.list || []
        this.total = this.schema.tree ? this.list.length : data.total || 0
      } catch (e) { if (id === this.loadId) this.error = e.message || '资料加载失败' }
      finally { if (id === this.loadId) this.loading = false }
    },
    search() { this.query.pageNum = 1; return this.load() },
    onSelectionChange(rows) {
      // Element UI may report only visible rows after paging; retain IDs selected on other pages.
      const visible = new Set(this.list.map(row => row.id))
      const ids = new Set(this.selectedIds.filter(id => !visible.has(id)))
      rows.forEach(row => ids.add(row.id))
      this.selectedIds = [...ids]
    },
    clearSelection() {
      this.selectedIds = []
      if (this.$refs.dataTable && this.$refs.dataTable.clearSelection) this.$refs.dataTable.clearSelection()
    },
    openImport() {
      this.importFile = null; this.importErrors = []; this.importMessage = ''; this.importResultType = 'success'; this.importVisible = true
      // Reset the native input so choosing the same corrected file triggers change again.
      this.$nextTick(() => { if (this.$refs.importFileInput) this.$refs.importFileInput.value = '' })
    },
    chooseImportFile(event) { this.importFile = event.target.files && event.target.files[0] || null; this.importErrors = []; this.importMessage = ''; this.importResultType = 'success' },
    async downloadTemplate() {
      if (this.templateLoading) return
      this.templateLoading = true
      try { await downloadFile(await this.api.importTemplate(), `${this.schema.title}导入模板.xlsx`) }
      catch (e) { this.error = e.message || '模板下载失败' }
      finally { this.templateLoading = false }
    },
    async submitImport() {
      if (this.importing || !this.importFile) return
      if (!/\.xlsx$/i.test(this.importFile.name) || this.importFile.size > 5 * 1024 * 1024) {
        this.importMessage = '请选择不超过 5 MB 的 XLSX 文件'; this.importResultType = 'error'; return
      }
      this.importing = true; this.importErrors = []; this.importMessage = ''
      try {
        const result = await this.api.importFile(this.importFile)
        this.importErrors = result.errors || []
        this.importResultType = this.importErrors.length ? 'error' : 'success'
        this.importMessage = this.importErrors.length ? `导入失败：${result.failedRows} 行需修正，整批未写入` : `成功导入 ${result.successRows} 行`
        if (!this.importErrors.length) { this.clearSelection(); await this.load() }
      } catch (e) {
        this.importErrors = e.importResult && e.importResult.errors || []
        this.importResultType = 'error'
        this.importMessage = e.message || '导入失败'
      } finally { this.importing = false }
    },
    reset() { this.query = { pageNum: 1, pageSize: this.query.pageSize, keyword: '', status: undefined }; return this.load() },
    newForm(parentId) {
      const form = { status: 1 }
      this.schema.fields.forEach(field => {
        if (field.key === 'status') return
        form[field.key] = field.type === 'parent' ? parentId : field.key === 'sortNo' ? 0 : field.type === 'number' || field.resource ? undefined : ''
      })
      return form
    },
    async open(row, mode = 'edit', parentId = 0) {
      const id = ++this.detailId
      this.originalStatus = null; this.readOnly = mode === 'view'; this.formError = ''; this.form = this.newForm(parentId)
      this.dialogVisible = true; this.detailLoading = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      try {
        const [detail, tree] = await Promise.all([
          row ? this.api.get(row.id) : Promise.resolve({}),
          this.schema.tree ? getCategoryTree() : Promise.resolve([])
        ])
        if (id !== this.detailId) return
        this.form = { ...this.form, ...detail }; this.originalStatus = detail.status; this.categoryTree = tree || []
      } catch (e) { if (id === this.detailId) { this.formError = e.message || '详情加载失败'; this.readOnly = true } }
      finally { if (id === this.detailId) this.detailLoading = false }
    },
    closeDialog(done) { if (!this.saving) { this.detailId++; done() } },
    async save() {
      if (this.saving || this.detailLoading || this.readOnly) return
      this.saving = true; this.formError = ''
      try {
        const valid = await new Promise(resolve => this.$refs.form.validate(resolve))
        if (!valid) return
        const status = this.canChangeFormStatus ? this.form.status : this.originalStatus
        if (this.form.id && this.originalStatus === 1 && status === 0) {
          try { await this.$confirm(`确认停用「${this.rowName(this.form)}」？停用后将无法用于新业务。`, '停用确认', { type: 'warning' }) } catch (e) { return }
        }
        const payload = this.schema.fields.reduce((data, field) => {
          const value = field.key === 'status' ? status : this.form[field.key]
          data[field.key] = field.type === 'number' && (value === undefined || value === '') ? null : value
          return data
        }, {})
        if (this.form.id) await this.api.update(this.form.id, payload)
        else await this.api.create(payload)
        this.dialogVisible = false; this.$message.success('保存成功'); await this.load()
      } catch (e) { this.formError = e.message || '保存失败，请重试' }
      finally { this.saving = false }
    },
    rowName(row) { return row[`${this.schema.key}Name`] || row.productName || this.schema.title },
    async changeStatus(row) {
      const status = row.status === 1 ? 0 : 1
      if (status === 0) {
        try { await this.$confirm(`确认停用「${this.rowName(row)}」？停用后将无法用于新业务。`, '停用确认', { type: 'warning' }) } catch (e) { return }
      }
      this.busyId = row.id; this.error = ''
      try { await this.api.status(row.id, status); this.$message.success(status === 1 ? '已启用' : '已停用'); await this.load() }
      catch (e) { this.error = e.message || '状态更新失败' }
      finally { this.busyId = null }
    },
    async remove(row) {
      try { await this.$confirm(`确认删除「${this.rowName(row)}」？`, '删除确认', { type: 'warning' }) } catch (e) { return }
      this.busyId = row.id; this.error = ''
      try {
        await this.api.remove(row.id)
        if (this.list.length === 1 && this.query.pageNum > 1) this.query.pageNum--
        this.$message.success('删除成功'); await this.load()
      } catch (e) { this.error = e.message || '删除失败' }
      finally { this.busyId = null }
    },
    async exportData() {
      this.exporting = true; this.error = ''
      try {
        const response = this.selectedIds.length ? await this.api.exportSelected(this.selectedIds) : await this.api.export(this.filters())
        await downloadFile(response, `${this.schema.title}.xlsx`)
      }
      catch (e) { this.error = e.message || '导出失败，请重试' }
      finally { this.exporting = false }
    }
  }
}
</script>

<style scoped>
.basic-page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; gap: 12px; }
.toolbar-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.toolbar-actions > .el-button + .el-button { margin-left: 0; }
.refresh-button { flex: none; }
.selection-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; padding: 10px 16px; border: 1px solid #b3d8ff; border-radius: 6px; background: #ecf5ff; color: #409eff; }
.selection-copy { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 13px; }
.selection-copy i { font-size: 17px; }
.selection-copy span { color: #606e80; }
.selection-clear { flex: none; padding: 0; }
.import-intro { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 16px; color: #606266; line-height: 1.6; }
.import-intro i { margin-top: 3px; color: #409eff; }
.import-upload { position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; min-height: 154px; border: 1px dashed #b8c8da; border-radius: 8px; background: #f8fbff; color: #409eff; cursor: pointer; transition: border-color .2s, background-color .2s; }
.import-upload:hover { border-color: #409eff; background: #f0f7ff; }
.import-upload.is-disabled { opacity: .6; cursor: not-allowed; }
.import-upload input { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }
.import-upload input:disabled { cursor: not-allowed; }
.import-upload i { font-size: 28px; }
.import-upload strong { max-width: 90%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.import-upload span { color: #909399; font-size: 12px; }
.import-template { margin: 6px 0 14px; color: #909399; font-size: 13px; }
.import-template .el-button { padding: 8px 0; }
.import-result { margin-bottom: 12px; }
@media (max-width: 768px) { .toolbar { flex-wrap: wrap; } .selection-bar { align-items: flex-start; } .selection-copy span { width: 100%; padding-left: 25px; } }
.error-banner { margin-bottom: 16px; }
.danger { color: #f56c6c; }
.permission-hint { color: #909399; font-size: 12px; }
.el-select, .el-input-number { width: 100%; }
</style>
