import { shallowMount } from '@vue/test-utils'
import BusinessOrderPage from '@/views/purchase/in/BusinessOrderPage'
import BasicDataSelect from '@/components/BasicDataSelect'
import { listInventory } from '@/api/inventory/inventory'

jest.mock('@/api/purchase/purchaseIn', () => ({ listPurchaseIn: jest.fn().mockResolvedValue({ list: [], total: 0 }) }))
jest.mock('@/api/sale/saleOut', () => ({ listSaleOut: jest.fn().mockResolvedValue({ list: [], total: 0 }) }))
jest.mock('@/api/inventory/inventory', () => ({ listInventory: jest.fn() }))
jest.mock('@/api/basic', () => ({ getProductUnits: jest.fn().mockResolvedValue([]) }))
const stubs = ['el-card', 'el-form', 'el-form-item', 'el-input', 'el-button', 'el-alert', 'el-table', 'el-table-column', 'el-dialog', 'el-row', 'el-col', 'el-date-picker', 'el-input-number']
const mount = (kind, permissions = ['*:*:*']) => shallowMount(BusinessOrderPage, { propsData: { kind },
  stubs: Object.assign({}, ...stubs.map(name => ({ [name]: true })), {
    'el-table-column': { render(h) { return h('div', this.$scopedSlots.default ? this.$scopedSlots.default({ row: { id: 2 }, $index: 0 }) : []) } }
  }), directives: { permission: {}, loading: {} }, mocks: { $store: { state: { user: { permissions } } } } })
const flush = async () => { await Promise.resolve(); await Promise.resolve() }

test.each([['purchase', 'suppliers'], ['sale', 'customers']])('%s filters use warehouse and partner name selectors', (kind, partnerResource) => {
  const wrapper = mount(kind)
  const resources = wrapper.findAllComponents(BasicDataSelect).wrappers.map(select => select.props('resource'))
  expect(resources).toContain('warehouses')
  expect(resources).toContain(partnerResource)
  wrapper.destroy()
})

test('product choice brings category, spec and base unit into a new purchase line', () => {
  const wrapper = mount('purchase'), row = wrapper.vm.itemDefaults()
  wrapper.vm.productChanged(row, { id: 1, name: '大米', categoryName: '粮食', spec: '25kg', baseUnitId: 4, baseUnitName: 'kg' })
  expect(row).toEqual(expect.objectContaining({ productName: '大米', categoryName: '粮食', spec: '25kg', unitId: 4, unitName: 'kg', convertRate: 1 }))
  wrapper.destroy()
})

test('sale stock hint discards an older warehouse request and displays current base quantity', async () => {
  let finishOld
  listInventory.mockImplementationOnce(() => new Promise(resolve => { finishOld = resolve })).mockResolvedValueOnce({ list: [{ availableQty: 7, unitName: 'kg' }] })
  const wrapper = mount('sale'), row = wrapper.vm.itemDefaults({ productId: 1 })
  wrapper.vm.form.warehouseId = 1
  wrapper.vm.refreshStock(row)
  wrapper.vm.form.warehouseId = 2
  await wrapper.vm.refreshStock(row)
  finishOld({ list: [{ availableQty: 50, unitName: 'kg' }] }); await flush()
  expect(row.availableQty).toBe(7)
  expect(row.stockUnitName).toBe('kg')
  expect(row.stockLoading).toBe(false)
  wrapper.destroy()
})

test.each(['purchase', 'sale'])('%s edit action needs both update and detail permissions', kind => {
  for (const [permissions, allowed] of [
    [[`${kind}:order:update`], false], [[`${kind}:order:view`], false],
    [[`${kind}:order:update`, `${kind}:order:view`], true], [['*:*:*'], true]
  ]) {
    const wrapper = mount(kind, permissions)
    const edit = wrapper.findAll('el-button-stub').wrappers.find(button => button.text() === '编辑')
    expect(Boolean(edit)).toBe(allowed)
    wrapper.destroy()
  }
})
