export function blankSaleOrder() {
  return { warehouseId: null, bizDate: new Date().toISOString().slice(0, 10), saleType: 'retail', fulfillmentType: 'pickup',
    customerId: null, customerName: '散客', contactName: '', contactPhone: '', deliveryAddress: '',
    plannedDeliveryTime: null, deliveryRemark: '', handlerName: '', remark: '', items: [] }
}

export function copySaleOrder(order) {
  const copy = { ...blankSaleOrder(), ...order, bizDate: new Date().toISOString().slice(0, 10) }
  const excluded = ['id', 'orderNo', 'orderStatus', 'completedTime', 'recordedTime', 'events']
  excluded.forEach(key => delete copy[key])
  copy.items = (order.items || []).map(row => { const item = { ...row, availableQty: null }; delete item.id; return item })
  return copy
}

export function totalAmount(items) {
  return (items || []).reduce((sum, row) => sum + Number(row.qty || 0) * Number(row.unitPrice || 0), 0).toFixed(2)
}

export function validateSaleOrder(form) {
  if (!form.warehouseId) return '请选择仓库'
  if (!form.bizDate) return '请选择录单日期'
  if (form.fulfillmentType === 'delivery') {
    if (!form.contactName) return '请填写收货人'
    if (!form.contactPhone) return '请填写收货电话'
    if (!form.deliveryAddress) return '请填写配送地址'
  }
  if (!form.items || !form.items.length) return '请添加商品明细'
  for (let i = 0; i < form.items.length; i++) {
    const row = form.items[i]
    if (!row.productId || !row.unitId) return `第 ${i + 1} 行请选择商品和单位`
    if (!(Number(row.qty) > 0)) return `第 ${i + 1} 行数量必须大于零`
    if (Number(row.unitPrice) < 0 || !Number.isFinite(Number(row.unitPrice))) return `第 ${i + 1} 行单价无效`
  }
  return ''
}

export function salePayload(form) {
  return { warehouseId: form.warehouseId, bizDate: form.bizDate, saleType: form.saleType,
    fulfillmentType: form.fulfillmentType, customerId: form.customerId || null,
    customerName: form.customerName || '散客', contactName: form.contactName || null,
    contactPhone: form.contactPhone || null, deliveryAddress: form.deliveryAddress || null,
    plannedDeliveryTime: form.plannedDeliveryTime || null, deliveryRemark: form.fulfillmentType === 'delivery' ? form.deliveryRemark || null : null,
    handlerName: form.handlerName || null,
    remark: form.remark || null,
    items: form.items.map(row => ({ productId: row.productId, unitId: row.unitId,
      qty: row.qty, unitPrice: row.unitPrice, amount: Number(row.qty) * Number(row.unitPrice), remark: row.remark || null })) }
}
