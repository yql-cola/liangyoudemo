import { shallowMount } from '@vue/test-utils'
import BasicDataPage from '@/views/basic/BasicDataPage.vue'
import { categoryParentOptions, filterCategoryTree } from '@/views/basic/categoryTree'
import { basicSchemas } from '@/views/basic/schemas'
import { createBasicApi, getCategoryTree } from '@/api/basic'
import { downloadFile } from '@/utils/download'

jest.mock('@/api/basic', () => ({ createBasicApi: jest.fn(), getCategoryTree: jest.fn() }))
jest.mock('@/utils/download', () => ({ downloadFile: jest.fn() }))
const flush = async () => { await Promise.resolve(); await Promise.resolve() }
const stubs = ['el-card', 'el-form', 'el-form-item', 'el-input', 'el-input-number', 'el-select', 'el-option', 'el-button', 'el-table', 'el-table-column', 'el-dialog', 'el-row', 'el-col', 'el-tag', 'el-alert']
let api
function mount(resource = 'product', permissions = ['*:*:*']) {
  return shallowMount(BasicDataPage, { propsData: { schema: basicSchemas[resource] }, stubs: Object.assign({}, ...stubs.map(name => ({ [name]: true })), {
    'el-form': { template: '<form><slot /></form>', methods: { clearValidate() {}, validate(callback) { callback(true) } } },
    'el-table-column': { render(h) { return h('div', this.$scopedSlots.default ? this.$scopedSlots.default({ row: { id: 2, status: 1 } }) : []) } }
  }), directives: { permission: {}, loading: {} }, mocks: { $message: { success: jest.fn() }, $store: { state: { user: { permissions } } } } })
}
beforeEach(() => {
  api = { list: jest.fn().mockResolvedValue({ list: [], total: 0 }), get: jest.fn().mockResolvedValue({ id: 2, productCode: 'P2', productName: '完整详情' }), export: jest.fn().mockResolvedValue({ data: 'file' }), exportSelected: jest.fn().mockResolvedValue({ data: 'file' }), importFile: jest.fn(), create: jest.fn(), update: jest.fn() }
  createBasicApi.mockReturnValue(api)
  getCategoryTree.mockResolvedValue([])
})

test('filter changes reset pagination and export sends all current filters without page limits', async () => {
  const wrapper = mount(); await flush()
  wrapper.vm.query.pageNum = 4
  wrapper.vm.query.keyword = '米'
  wrapper.vm.query.status = 0
  await wrapper.vm.search()
  expect(api.list).toHaveBeenLastCalledWith(expect.objectContaining({ pageNum: 1, keyword: '米', status: 0 }))
  await wrapper.vm.exportData()
  expect(api.export).toHaveBeenCalledWith({ keyword: '米', status: 0 })
  expect(downloadFile).toHaveBeenCalled()
})

test('selected records export only their ids across pages', async () => {
  const wrapper = mount('unit'); await flush()
  wrapper.vm.onSelectionChange([{ id: 2 }])
  wrapper.vm.query.pageNum = 2
  wrapper.vm.onSelectionChange([{ id: 4 }])
  await wrapper.vm.exportData()
  expect(api.exportSelected).toHaveBeenCalledWith([2, 4])
})

test('create permission exposes the import action and selection stays in its own bar', async () => {
  const wrapper = mount('category', ['basic:category:create']); await flush()
  expect(wrapper.findAll('el-button-stub').wrappers.some(button => button.text() === '批量导入')).toBe(true)
  wrapper.vm.onSelectionChange([{ id: 2 }]); await wrapper.vm.$nextTick()
  expect(wrapper.find('.selection-bar').exists()).toBe(true)
  expect(wrapper.find('.toolbar').text()).not.toContain('已选')
  wrapper.destroy()
})

test('list permission alone does not expose import actions', async () => {
  const wrapper = mount('category', ['basic:category:list']); await flush()
  const buttons = wrapper.findAll('el-button-stub').wrappers.map(button => button.text())
  expect(buttons).not.toContain('批量导入')
  expect(buttons).not.toContain('下载导入模板')
  wrapper.destroy()
})

test('import reports spreadsheet row errors without refreshing the list', async () => {
  const wrapper = mount('unit'); await flush()
  const file = new File(['xlsx'], 'units.xlsx')
  api.importFile.mockResolvedValue({ totalRows: 2, successRows: 0, failedRows: 2,
    errors: [{ rowNumber: 3, columnName: '单位编码', message: '文件内记录重复' }] })
  wrapper.vm.importFile = file
  await wrapper.vm.submitImport()
  expect(api.importFile).toHaveBeenCalledWith(file)
  expect(wrapper.vm.importErrors[0].rowNumber).toBe(3)
  expect(api.list).toHaveBeenCalledTimes(1)
})

test('detail dialog reads full backend data and remains read only', async () => {
  const wrapper = mount(); await flush()
  await wrapper.vm.open({ id: 2, productName: '列表摘要' }, 'view')
  expect(api.get).toHaveBeenCalledWith(2)
  expect(wrapper.vm.form.productName).toBe('完整详情')
  expect(wrapper.vm.readOnly).toBe(true)
  expect(wrapper.findAll('el-select-stub').at(1).attributes('disabled')).toBe('true')
  expect(wrapper.findAll('el-button-stub').wrappers.some(button => button.text() === '保存')).toBe(false)
  await wrapper.vm.save()
  expect(api.update).not.toHaveBeenCalled()
})

test('category parent selection includes every level and excludes edited subtree', () => {
  const tree = [{ id: 1, categoryName: '粮食', children: [{ id: 2, categoryName: '大米', children: [{ id: 3, categoryName: '粳米' }] }] }, { id: 4, categoryName: '食用油' }]
  expect(categoryParentOptions(tree, 2).map(item => item.id)).toEqual([0, 1, 4])
  expect(categoryParentOptions(tree).map(item => item.id)).toEqual([0, 1, 2, 3, 4])
})


test('category keyword/status filtering keeps ancestors of matching descendants', () => {
  const tree = [{ id: 1, categoryName: '粮食', status: 1, children: [{ id: 2, categoryName: '大米', status: 0 }, { id: 3, categoryName: '面粉', status: 1 }] }]
  const filtered = filterCategoryTree(tree, { keyword: '米', status: 0 })
  expect(filtered[0].children.map(item => item.id)).toEqual([2])
  expect(tree[0].children).toHaveLength(2)
})

test('canceling disable in edit form preserves dialog and does not save', async () => {
  api.get.mockResolvedValue({ id: 2, status: 1, unitCode: 'BAG', unitName: '袋' })
  const wrapper = mount('unit'); await flush()
  wrapper.vm.$confirm = jest.fn().mockRejectedValue('cancel')
  await wrapper.vm.open({ id: 2 })
  wrapper.vm.form.status = 0
  await wrapper.vm.save()
  expect(wrapper.vm.$confirm).toHaveBeenCalled()
  expect(api.update).not.toHaveBeenCalled()
  expect(wrapper.vm.dialogVisible).toBe(true)
})

test('save sends only editable fields and keeps the form open when backend validation fails', async () => {
  api.get.mockResolvedValue({ id: 2, status: 1, unitCode: 'BAG', unitName: '袋', version: 10, createdTime: '2026-10-17' })
  api.update.mockRejectedValue(new Error('单位编码已存在'))
  const wrapper = mount('unit'); await flush()
  await wrapper.vm.open({ id: 2 })
  await wrapper.vm.save()
  expect(api.update).toHaveBeenCalledWith(2, { unitCode: 'BAG', unitName: '袋', status: 1, remark: '' })
  expect(wrapper.vm.formError).toBe('单位编码已存在')
  expect(wrapper.vm.dialogVisible).toBe(true)
  expect(wrapper.vm.saving).toBe(false)
})

test('a failed query keeps existing rows and releases loading state for retry', async () => {
  api.list.mockResolvedValueOnce({ list: [{ id: 1, productName: '大米' }], total: 1 }).mockRejectedValueOnce(new Error('网络连接失败'))
  const wrapper = mount(); await flush()
  await wrapper.vm.load()
  expect(wrapper.vm.list[0].productName).toBe('大米')
  expect(wrapper.vm.error).toBe('网络连接失败')
  expect(wrapper.vm.loading).toBe(false)
})

test('repeated save clicks during asynchronous validation create only one record', async () => {
  const wrapper = mount('unit'); await flush()
  await wrapper.vm.open()
  let finishValidation
  wrapper.vm.$refs.form.validate = jest.fn(callback => { finishValidation = callback })
  const first = wrapper.vm.save()
  const second = wrapper.vm.save()
  expect(wrapper.vm.$refs.form.validate).toHaveBeenCalledTimes(1)
  finishValidation(true)
  await Promise.all([first, second])
  expect(api.create).toHaveBeenCalledTimes(1)
  expect(wrapper.vm.saving).toBe(false)
  wrapper.destroy()
})

test('failed form validation releases the save lock without submitting', async () => {
  const wrapper = mount('unit'); await flush()
  await wrapper.vm.open()
  wrapper.vm.$refs.form.validate = callback => callback(false)
  await wrapper.vm.save()
  expect(api.create).not.toHaveBeenCalled()
  expect(wrapper.vm.saving).toBe(false)
  expect(wrapper.vm.dialogVisible).toBe(true)
  wrapper.destroy()
})

test.each([
  [['basic:unit:update'], false],
  [['basic:unit:view'], false],
  [['basic:unit:update', 'basic:unit:view'], true],
  [['*:*:*'], true]
])('basic edit action needs both update and detail permissions: %j', (permissions, allowed) => {
  const wrapper = mount('unit', permissions)
  const edit = wrapper.findAll('el-button-stub').wrappers.find(button => button.text() === '编辑')
  expect(Boolean(edit)).toBe(allowed)
  wrapper.destroy()
})

test('edit without status permission disables status and preserves the original value in its payload', async () => {
  api.get.mockResolvedValue({ id: 2, status: 1, unitCode: 'BAG', unitName: '袋' })
  const wrapper = mount('unit', ['basic:unit:view', 'basic:unit:update']); await flush()
  await wrapper.vm.open({ id: 2 }); await wrapper.vm.$nextTick()
  expect(wrapper.findAll('el-select-stub').at(1).attributes('disabled')).toBe('true')
  expect(wrapper.text()).toContain('需要启停用权限')
  wrapper.vm.form.status = 0
  await wrapper.vm.save()
  expect(api.update).toHaveBeenCalledWith(2, expect.objectContaining({ status: 1 }))
  wrapper.destroy()
})

test.each([['basic:unit:status'], ['*:*:*']])('status permission permits changing an existing status: %s', async permission => {
  api.get.mockResolvedValue({ id: 2, status: 0, unitCode: 'BAG', unitName: '袋' })
  const wrapper = mount('unit', [permission]); await flush()
  await wrapper.vm.open({ id: 2 }); await wrapper.vm.$nextTick()
  expect(wrapper.findAll('el-select-stub').at(1).attributes('disabled')).toBeUndefined()
  wrapper.vm.form.status = 1
  await wrapper.vm.save()
  expect(api.update).toHaveBeenCalledWith(2, expect.objectContaining({ status: 1 }))
  wrapper.destroy()
})

test('creation can set initial status with create permission alone', async () => {
  const wrapper = mount('unit', ['basic:unit:create']); await flush()
  await wrapper.vm.open(); await wrapper.vm.$nextTick()
  expect(wrapper.findAll('el-select-stub').at(1).attributes('disabled')).toBeUndefined()
  wrapper.vm.form.status = 0
  await wrapper.vm.save()
  expect(api.create).toHaveBeenCalledWith(expect.objectContaining({ status: 0 }))
  wrapper.destroy()
})
