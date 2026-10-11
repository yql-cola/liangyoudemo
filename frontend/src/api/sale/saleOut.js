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
