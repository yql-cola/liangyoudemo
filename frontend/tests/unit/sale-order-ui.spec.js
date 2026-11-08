import { shallowMount } from '@vue/test-utils'
import SaleOrderEditor from '@/views/sale/out/SaleOrderEditor.vue'
import { addSaleOut, saleAction } from '@/api/sale/saleOut'

jest.mock('@/api/sale/saleOut', () => ({ getSaleOut: jest.fn(), addSaleOut: jest.fn(), updateSaleOut: jest.fn(),
  saleAction: jest.fn(), searchCustomers: jest.fn().mockResolvedValue([]), quickCustomer: jest.fn() }))

describe('sale order editor', () => {
  it('keeps a manually edited price when the sale unit changes', () => {
    const wrapper = shallowMount(SaleOrderEditor, { mocks: { $route: { params: {} }, $router: { push: jest.fn() } } })
    const row = { productId: null, unitId: null, qty: 1, unitPrice: null, priceMode: 'auto' }
    wrapper.vm.productChanged(row, { id: 2, name: '大米', baseUnitId: 3, baseUnitName: '袋', defaultSalePrice: 12 })
    expect(row.unitPrice).toBe(12)
    row.unitPrice = 11
    wrapper.vm.priceChanged(row)
    wrapper.vm.unitChanged(row, { name: '箱', convertRate: 10 })
    expect(row.unitPrice).toBe(11)
  })
  it('offers guest pickup and delivery recipient fields', async () => {
    const wrapper = shallowMount(SaleOrderEditor, { mocks: { $route: { params: {} }, $router: { push: jest.fn() }, $store: { state: { user: { permissions: ['sale:order:create'] } } } } })
    expect(wrapper.vm.form.customerId).toBeNull()
    expect(wrapper.vm.form.fulfillmentType).toBe('pickup')
    wrapper.vm.form.fulfillmentType = 'delivery'
    await wrapper.vm.$nextTick()
    expect(wrapper.html()).toContain('label="收货人"')
  })
  it('keeps confirmation available after stock rejection on a newly saved draft', async () => {
    addSaleOut.mockResolvedValue(42)
    saleAction.mockRejectedValue(new Error('库存不足'))
    const wrapper = shallowMount(SaleOrderEditor, { mocks: { $route: { params: {} }, $router: { push: jest.fn() } } })
    wrapper.vm.form.warehouseId = 1
    wrapper.vm.form.items = [{ productId: 2, unitId: 3, qty: 1, unitPrice: 4 }]
    await wrapper.vm.save(true)
    expect(wrapper.vm.form.id).toBe(42)
    expect(wrapper.vm.form.orderStatus).toBe('draft')
    expect(wrapper.vm.error).toBe('库存不足')
    expect(wrapper.html()).toContain('确认订单')
  })
})
