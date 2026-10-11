import request from '@/utils/request'

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
