import { blankSaleOrder, validateSaleOrder, salePayload, totalAmount, copySaleOrder } from '@/views/sale/out/saleOrderForm'

describe('sale order form', () => {
  it('allows a pickup guest without a customer record', () => {
    const form = blankSaleOrder()
    form.warehouseId = 1
    form.items.push({ productId: 2, unitId: 3, qty: 2, unitPrice: 4 })
    expect(validateSaleOrder(form)).toBe('')
    expect(salePayload(form).customerId).toBeNull()
    expect(totalAmount(form.items)).toBe('8.00')
  })
  it('requires recipient details for delivery', () => {
    const form = blankSaleOrder()
    form.warehouseId = 1
    form.fulfillmentType = 'delivery'
    form.items.push({ productId: 2, unitId: 3, qty: 2, unitPrice: 4 })
    expect(validateSaleOrder(form)).toContain('收货人')
  })
  it('includes delivery notes only for delivery orders', () => {
    const form = blankSaleOrder()
    form.deliveryRemark = '后门卸货'
    form.items.push({ productId: 2, unitId: 3, qty: 1, unitPrice: 4 })
    expect(salePayload(form).deliveryRemark).toBeNull()
    form.fulfillmentType = 'delivery'
    expect(salePayload(form).deliveryRemark).toBe('后门卸货')
  })
  it('copies a returned order into a new editable draft', () => {
    const copy = copySaleOrder({ id: 9, orderStatus: 'returned', customerName: '客户甲',
      items: [{ id: 4, productId: 2, unitId: 3, qty: 1, unitPrice: 8 }] })
    expect(copy.id).toBeUndefined()
    expect(copy.items[0].id).toBeUndefined()
    expect(copy.items[0].productId).toBe(2)
  })
})
