import { validDate, localDateTime } from '@/views/purchase/in/orderForm'

export const scenarios = {
  dashboard: { title: '经营看板', customers: true, suppliers: true },
  trend: { title: '进销趋势分析', timeSeries: true, customers: true, suppliers: true },
  'sale-product': { title: '商品销售分析', customers: true },
  'purchase-product': { title: '商品采购分析', suppliers: true },
  'inventory-structure': { title: '库存结构分析' },
  'customer-rank': { title: '客户销售排行', customers: true },
  'supplier-rank': { title: '供应商采购排行', suppliers: true },
  'category-share': { title: '分类占比分析', customers: true, suppliers: true }
}

export const isInventory = (scenario, query) => scenario === 'inventory-structure' || scenario === 'category-share' && query.statType === 'inventory'

export function defaultStatisticsQuery(scenario, groupBy = 'day') {
  const now = new Date(), start = new Date(now)
  start.setDate(now.getDate() - 29)
  return { dateFrom: localDateTime(start).slice(0, 10), dateTo: localDateTime(now).slice(0, 10), warehouseId: undefined, productId: undefined, categoryId: undefined,
    customerId: undefined, supplierId: undefined, statType: scenario === 'category-share' ? 'sale' : undefined, groupBy, dimension: 'product' }
}

export function statisticsParams(scenario, query) {
  const keys = ['warehouseId', 'productId', 'categoryId']
  if (!isInventory(scenario, query)) {
    keys.push('dateFrom', 'dateTo', 'groupBy')
    if (scenarios[scenario].customers && query.statType !== 'purchase') keys.push('customerId')
    if (scenarios[scenario].suppliers && query.statType !== 'sale') keys.push('supplierId')
  }
  if (scenario === 'trend' || scenario === 'category-share') keys.push('statType')
  if (scenario === 'inventory-structure') keys.push('dimension')
  return keys.reduce((params, key) => {
    if (query[key] !== undefined && query[key] !== null && query[key] !== '') params[key] = query[key]
    return params
  }, {})
}

export function validateStatisticsQuery(scenario, query) {
  if (isInventory(scenario, query)) return ''
  if ((query.dateFrom && !validDate(query.dateFrom)) || (query.dateTo && !validDate(query.dateTo))) return '请选择有效的开始和结束日期'
  return query.dateFrom && query.dateTo && query.dateFrom > query.dateTo ? '开始日期不能晚于结束日期' : ''
}

const number = value => Number(value || 0)
export function chartOption(rows, type, metric = 'amount', top = 10, timeSeries = false) {
  const source = timeSeries ? [...rows] : [...rows].sort((a, b) => number(b[metric]) - number(a[metric]))
  const data = timeSeries || type !== 'bar' || !top ? source : source.slice(0, top)
  const label = metric === 'amount' ? '金额（元）' : '基础数量'
  const names = data.map(row => row.dimensionName || row.dimensionCode || '未命名')
  if (type === 'pie') return {
    tooltip: { trigger: 'item', valueFormatter: value => `${value}${metric === 'amount' ? ' 元' : ' 基础数量'}` },
    legend: { type: 'scroll', bottom: 0 },
    series: [{ name: label, type: 'pie', radius: ['32%', '62%'], center: ['50%', '45%'], data: data.map((row, i) => ({ name: names[i], value: number(row[metric]) })) }]
  }
  const split = timeSeries && data.some(row => row.purchaseAmount != null || row.saleAmount != null)
  const suffix = metric === 'amount' ? 'Amount' : 'Qty'
  const series = split ? ['purchase', 'sale'].map((key, index) => ({ name: index ? '销售' : '采购', type, data: data.map(row => number(row[`${key}${suffix}`])), smooth: false }))
    : [{ name: label, type, data: data.map(row => number(row[metric])), smooth: false }]
  return { tooltip: { trigger: 'axis' }, legend: { top: 0 }, grid: { top: 48, left: 65, right: 24, bottom: 75, containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: timeSeries ? 0 : 25, hideOverlap: true } }, yAxis: { type: 'value', name: label },
    dataZoom: data.length > 20 ? [{ type: 'slider', start: 0, end: Math.min(100, 20 / data.length * 100) }] : [], series }
}

export function statisticsColumns(scenario, inventory = false, timeSeries = false) {
  if (timeSeries) return [{ prop: 'dimensionName', label: '日期' },
    { prop: 'purchaseAmount', label: '采购金额', money: true }, { prop: 'saleAmount', label: '销售金额', money: true },
    { prop: 'purchaseQty', label: '采购基础数量' }, { prop: 'saleQty', label: '销售基础数量' }, { prop: 'unitName', label: '基础单位' }]
  const label = { 'customer-rank': '客户', 'supplier-rank': '供应商', 'category-share': '分类' }[scenario] || '名称'
  return [{ prop: 'rankNo', label: '序号', width: 70 }, { prop: 'dimensionCode', label: '编码' }, { prop: 'dimensionName', label },
    { prop: 'categoryName', label: '分类' }, { prop: 'spec', label: '规格' }, { prop: 'warehouseName', label: '仓库' },
    ...(scenario === 'purchase-product' ? [{ prop: 'supplierName', label: '供应商' }] : []),
    { prop: 'amount', label: inventory ? '库存价值预估' : '金额', money: true }, { prop: 'qty', label: '基础数量' }, { prop: 'unitName', label: '基础单位' },
    { prop: 'amountRate', label: '金额占比', rate: true }, { prop: 'qtyRate', label: '基础数量占比', rate: true },
    ...(inventory ? [{ prop: 'availableQty', label: '可用基础数量' }, { prop: 'lockedQty', label: '锁定基础数量' }]
      : [{ prop: 'averagePrice', label: '基础单位均价', money: true }, { prop: 'orderCount', label: '单据数' }])]
}
