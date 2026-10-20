/* global BigInt */
export function orderAmount(qty, price) {
  if (!Number.isFinite(Number(qty)) || !Number.isFinite(Number(price)) || Number(qty) < 0 || Number(price) < 0) return '0.00'
  const scaled = value => BigInt(Number(value).toFixed(4).replace('.', ''))
  const cents = (scaled(qty) * scaled(price) + BigInt(500000)) / BigInt(1000000)
  return `${cents / BigInt(100)}.${String(cents % BigInt(100)).padStart(2, '0')}`
}

export function validDate(value, datetime = false) {
  if (typeof value !== 'string' || !(datetime ? /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/ : /^\d{4}-\d{2}-\d{2}$/).test(value)) return false
  const date = new Date(`${value}${datetime ? 'Z' : 'T00:00:00Z'}`)
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, datetime ? 19 : 10) === value
}

export function validateOrder(form, kind) {
  if (!form.warehouseId) return '请选择仓库'
  if (!form[kind === 'purchase' ? 'supplierId' : 'customerId']) return `请选择${kind === 'purchase' ? '供应商' : '客户'}`
  if (!validDate(form.bizDate)) return '请选择有效的业务日期'
  if (!form.items || !form.items.length) return '请至少添加一条明细'
  for (let i = 0; i < form.items.length; i++) {
    const row = form.items[i], prefix = `第 ${i + 1} 行：`
    if (!row.productId || !row.unitId) return `${prefix}请选择商品及单位`
    if (!Number.isFinite(Number(row.qty)) || Number(row.qty) <= 0) return `${prefix}数量必须大于零`
    if (row.unitPrice === undefined || row.unitPrice === null || row.unitPrice === '' || !Number.isFinite(Number(row.unitPrice)) || Number(row.unitPrice) < 0) return `${prefix}单价必须为非负数`
    if (kind === 'purchase') {
      if (!String(row.batchNo || '').trim()) return `${prefix}请填写批次号`
      if (!validDate(row.inboundTime, true)) return `${prefix}请选择有效的入库时间`
      if (row.productionDate && !validDate(row.productionDate)) return `${prefix}生产日期无效`
      if (row.expiryDate && !validDate(row.expiryDate)) return `${prefix}过期日期无效`
      if (row.productionDate && row.expiryDate && row.expiryDate < row.productionDate) return `${prefix}过期日期不能早于生产日期`
    }
  }
  return ''
}

export function buildOrderPayload(form, kind) {
  const partner = kind === 'purchase' ? 'supplierId' : 'customerId'
  return { warehouseId: form.warehouseId, [partner]: form[partner], bizDate: form.bizDate, status: form.status, remark: form.remark || '',
    items: form.items.map(row => {
      const item = { productId: row.productId, unitId: row.unitId, qty: row.qty, unitPrice: row.unitPrice, amount: orderAmount(row.qty, row.unitPrice), remark: row.remark || '' }
      if (kind === 'purchase') Object.assign(item, { batchNo: row.batchNo.trim(), inboundTime: row.inboundTime, productionDate: row.productionDate || null, expiryDate: row.expiryDate || null })
      return item
    }) }
}

export function localDateTime(date = new Date()) {
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
