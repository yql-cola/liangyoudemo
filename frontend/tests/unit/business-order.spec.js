import { orderAmount, buildOrderPayload, validateOrder } from '@/views/purchase/in/orderForm'

const purchase = () => ({ id: 8, warehouseId: 1, supplierId: 2, bizDate: '2026-10-17', status: 1, remark: '', items: [{ productId: 3, unitId: 4, qty: 1.005, unitPrice: 1, batchNo: 'B1', inboundTime: '2026-10-17T12:00:00', productionDate: '2026-10-01', expiryDate: '2026-12-01', productName: '大米' }] })

test('line amounts use exact decimal rounding and payload retains business IDs without display metadata', () => {
  expect(orderAmount(1.005, 1)).toBe('1.01')
  expect(orderAmount(3, 0.1)).toBe('0.30')
  const payload = buildOrderPayload(purchase(), 'purchase')
  expect(payload.items[0].amount).toBe('1.01')
  expect(payload.items[0]).not.toHaveProperty('productName')
  expect(payload.warehouseId).toBe(1)
  expect(payload).not.toHaveProperty('id')
})

test('purchase validation rejects zero qty, negative prices, missing batch, and reversed or invalid dates', () => {
  const form = purchase()
  expect(validateOrder(form, 'purchase')).toBe('')
  form.items[0].qty = 0
  expect(validateOrder(form, 'purchase')).toContain('数量')
  form.items[0].qty = 1; form.items[0].unitPrice = -1
  expect(validateOrder(form, 'purchase')).toContain('单价')
  form.items[0].unitPrice = 1; form.items[0].expiryDate = '2026-09-01'
  expect(validateOrder(form, 'purchase')).toContain('过期日期')
  form.items[0].expiryDate = '2026-12-01'; form.items[0].inboundTime = '2026-02-30T12:00:00'
  expect(validateOrder(form, 'purchase')).toContain('入库时间')
})

test('sales validation requires customer and at least one fully selected line', () => {
  expect(validateOrder({ warehouseId: 1, bizDate: '2026-10-17', items: [] }, 'sale')).toContain('客户')
  expect(validateOrder({ warehouseId: 1, customerId: 2, bizDate: '2026-10-17', items: [] }, 'sale')).toContain('明细')
})
