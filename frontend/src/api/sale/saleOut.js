import request from '@/utils/request'

export function listSaleOut(params) {
  return request({ url: '/v1/sale-outs', method: 'get', params })
}

export function getSaleOut(id) {
  return request({ url: `/v1/sale-outs/${id}`, method: 'get' })
}

export function addSaleOut(data) {
  return request({ url: '/v1/sale-outs', method: 'post', data })
}

export function updateSaleOut(id, data) {
  return request({ url: `/v1/sale-outs/${id}`, method: 'put', data })
}

export function deleteSaleOut(id) {
  return request({ url: `/v1/sale-outs/${id}`, method: 'delete' })
}

export function saleAction(id, action, data) {
  return request({ url: `/v1/sale-outs/${id}/${action}`, method: 'post', data })
}

export function exportSale(id, type) {
  return request({ url: `/v1/sale-outs/${id}/export/${type}`, method: 'get', responseType: 'blob' })
}

export function deliveryPdf(id) {
  return request({ url: `/v1/sale-outs/${id}/delivery-pdf`, method: 'get', responseType: 'blob' })
}

export function searchCustomers(keyword) {
  return request({ url: '/v1/basic/customers/search', method: 'get', params: { keyword } })
}

export function quickCustomer(data) {
  return request({ url: '/v1/basic/customers/quick', method: 'post', data })
}
