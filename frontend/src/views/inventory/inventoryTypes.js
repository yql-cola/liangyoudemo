// API and database keep stable codes; tables render these labels for operators.
export const inventoryTypeLabels = Object.freeze({
  purchase_in: '采购入库',
  purchase_in_reverse: '采购入库撤销',
  sale_out: '销售出库',
  sale_out_reverse: '销售出库撤销',
  inventory_adjust: '库存调整',
  adjust: '库存调整'
})

export const inventoryTypeLabel = code => inventoryTypeLabels[code] || code || ''
