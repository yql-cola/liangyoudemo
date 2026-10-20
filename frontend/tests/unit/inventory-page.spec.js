import { validateAdjustment } from '@/views/inventory/adjustment'
import { shallowMount } from '@vue/test-utils'
import InventoryPage from '@/views/inventory/InventoryPage'
import { inventoryTypeLabel } from '@/views/inventory/inventoryTypes'
import { listInventoryBatches, listInventory, updateInventory } from '@/api/inventory/inventory'

jest.mock('@/api/inventory/inventory', () => ({ listInventoryBatches: jest.fn().mockResolvedValue([]), listInventory: jest.fn().mockResolvedValue({ list: [], total: 0 }), updateInventory: jest.fn() }))
const stubs = ['el-card', 'el-form', 'el-form-item', 'el-button', 'el-alert', 'el-table', 'el-table-column', 'el-dialog', 'el-input', 'el-input-number', 'el-date-picker']
const mount = kind => shallowMount(InventoryPage, { propsData: { kind }, stubs, directives: { permission: {}, loading: {} }, mocks: { $route: { query: {} }, $message: { success: jest.fn() } } })

test('stock adjustment requires reason and total equals available plus unchanged locked', () => {
  expect(validateAdjustment({ totalQty: 12, availableQty: 10, lockedQty: 2, remark: '盘点' })).toBe('')
  expect(validateAdjustment({ totalQty: 12, availableQty: 10, lockedQty: 2, remark: ' ' })).toContain('原因')
  expect(validateAdjustment({ totalQty: 13, availableQty: 10, lockedQty: 2, remark: '盘点' })).toContain('锁定')
  expect(validateAdjustment({ totalQty: -1, availableQty: 0, lockedQty: 0, remark: '盘点' })).toContain('非负')
})

test('ledger type codes have Chinese labels including reversals', () => {
  expect(inventoryTypeLabel('purchase_in_reverse')).toBe('采购入库撤销')
  expect(inventoryTypeLabel('inventory_adjust')).toBe('库存调整')
  expect(inventoryTypeLabel('unknown_code')).toBe('unknown_code')
})

test('batch collection loads without a product and sends named selector filters', async () => {
  const wrapper = mount('batch')
  wrapper.vm.query.categoryId = 3; wrapper.vm.query.warehouseId = 2
  await wrapper.vm.search()
  expect(listInventoryBatches).toHaveBeenLastCalledWith(expect.objectContaining({ categoryId: 3, warehouseId: 2 }))
  wrapper.destroy()
})

test('stock save rejects inconsistent totals and preserves immutable locked quantity', async () => {
  const wrapper = mount('stock')
  wrapper.vm.open({ id: 1, totalQty: 12, availableQty: 10, lockedQty: 2 })
  wrapper.vm.form.remark = '盘点'; wrapper.vm.form.totalQty = 15
  await wrapper.vm.save()
  expect(updateInventory).not.toHaveBeenCalled()
  wrapper.vm.form.availableQty = 13
  await wrapper.vm.save()
  expect(updateInventory).toHaveBeenCalledWith(1, { totalQty: 15, availableQty: 13, remark: '盘点' })
  expect(listInventory).toHaveBeenCalled()
  wrapper.destroy()
})
