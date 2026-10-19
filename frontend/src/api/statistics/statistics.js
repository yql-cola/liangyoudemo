import request from '@/utils/request'

export const statisticsEndpoints = {
  dashboard: 'dashboard', trend: 'trends', 'sale-product': 'sale-products', 'purchase-product': 'purchase-products',
  'inventory-structure': 'inventory-structure', 'customer-rank': 'customer-ranks', 'supplier-rank': 'supplier-ranks', 'category-share': 'category-shares'
}
export const getStatistics = (scenario, params) => request({ url: `/v1/statistics/${statisticsEndpoints[scenario]}`, method: 'get', params })
export const exportStatistics = (scenario, params) => request({ url: `/v1/statistics/${statisticsEndpoints[scenario]}/export`, method: 'get', params, responseType: 'blob' })

export function dailyInboundOutbound(params) {
  return request({ url: '/v1/statistics/daily/inbound-outbound', method: 'get', params })
}

export function monthlyInboundOutbound(params) {
  return request({ url: '/v1/statistics/monthly/inbound-outbound', method: 'get', params })
}

export function amountByCustomer(params) {
  return request({ url: '/v1/statistics/amount-by-customer', method: 'get', params })
}

export function amountBySupplier(params) {
  return request({ url: '/v1/statistics/amount-by-supplier', method: 'get', params })
}

export function amountByCategory(params) {
  return request({ url: '/v1/statistics/amount-by-category', method: 'get', params })
}
