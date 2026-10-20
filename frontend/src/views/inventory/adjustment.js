export function validateAdjustment(form) {
  if (!String(form.remark || '').trim()) return '请填写库存调整原因'
  const values = [form.totalQty, form.availableQty, form.lockedQty]
  if (values.some(value => value === null || value === undefined || value === '' || !Number.isFinite(Number(value)) || Number(value) < 0)) return '库存数量必须为非负数'
  const [total, available, locked] = values.map(value => Math.round(Number(value) * 10000))
  if (total !== available + locked) return '总库存必须等于可用库存加锁定数量，锁定数量保持不变'
  return ''
}
