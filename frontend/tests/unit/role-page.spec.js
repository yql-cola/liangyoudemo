import { shallowMount } from '@vue/test-utils'
import RolePage from '@/views/system/role/index'
import { listRole, updateRole, getAssignablePermissions, getRolePermissionIds } from '@/api/system/role'

jest.mock('@/api/system/role', () => ({
  listRole: jest.fn().mockResolvedValue([]),
  addRole: jest.fn().mockResolvedValue({}),
  updateRole: jest.fn().mockResolvedValue({}),
  deleteRole: jest.fn(),
  assignRolePermissions: jest.fn(),
  getAssignablePermissions: jest.fn().mockResolvedValue([]),
  getRolePermissionIds: jest.fn().mockResolvedValue([])
}))

const TreeStub = {
  name: 'el-tree',
  data: () => ({ checked: [] }),
  methods: {
    setCheckedKeys(keys) { this.checked = keys },
    getCheckedKeys() { return this.checked }
  },
  render(h) { return h('div') }
}
const FormStub = {
  name: 'el-form',
  methods: { validate() { return Promise.resolve(true) } },
  render(h) { return h('div', this.$slots.default) }
}
const stubs = {
  'el-tree': TreeStub, 'el-form': FormStub,
  'el-card': true, 'el-form-item': true, 'el-input': true, 'el-select': true, 'el-option': true,
  'el-button': true, 'el-table': true, 'el-table-column': true, 'el-dialog': FormStub,
  'el-alert': true, 'el-empty': true
}
const flush = async () => { await Promise.resolve(); await Promise.resolve() }
const mount = permissions => shallowMount(RolePage, {
  stubs,
  directives: { permission: {}, loading: {} },
  mocks: { $store: { state: { user: { permissions } } }, $message: { success: jest.fn() } }
})

beforeEach(() => {
  jest.clearAllMocks()
  listRole.mockResolvedValue([])
  getAssignablePermissions.mockResolvedValue([{ id: 1, permissionName: '角色管理', status: 1, children: [
    { id: 2, permissionName: '角色授权', status: 1, children: [] },
    { id: 3, permissionName: '旧权限', status: 0, children: [] }
  ] }])
  getRolePermissionIds.mockResolvedValue([2, 3])
})

test('editing retains exact checked IDs and warns about disabled grants', async () => {
  const wrapper = mount(['system:role:grant'])
  wrapper.vm.open({ id: 8, roleCode: 'WAREHOUSE', roleName: '仓库', status: 1 })
  await flush()
  await wrapper.vm.$nextTick()
  await flush()
  expect(wrapper.vm.dialogDisabled.map(node => node.id)).toEqual([3])
  expect(wrapper.vm.$refs.roleTree.getCheckedKeys()).toEqual([2])
  await wrapper.vm.save()
  expect(updateRole).toHaveBeenCalledWith(8, expect.objectContaining({ permissionIds: [2] }))
  wrapper.destroy()
})

test('role editor without grant authority does not submit permissionIds', async () => {
  const wrapper = mount(['system:role:update'])
  wrapper.vm.open({ id: 8, roleCode: 'WAREHOUSE', roleName: '仓库', status: 1 })
  await wrapper.vm.save()
  expect(updateRole).toHaveBeenCalledWith(8, expect.not.objectContaining({ permissionIds: expect.anything() }))
  expect(getAssignablePermissions).not.toHaveBeenCalled()
  wrapper.destroy()
})

test('failed permission lookup blocks overwrite on edit', async () => {
  getRolePermissionIds.mockRejectedValueOnce(new Error('network'))
  const wrapper = mount(['system:role:grant'])
  wrapper.vm.open({ id: 8, roleCode: 'WAREHOUSE', roleName: '仓库', status: 1 })
  await flush()
  await wrapper.vm.$nextTick()
  await flush()
  expect(wrapper.vm.dialogLoadFailed).toBe(true)
  await wrapper.vm.save()
  expect(updateRole).not.toHaveBeenCalled()
  wrapper.destroy()
})
