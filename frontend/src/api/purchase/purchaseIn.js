import request from '@/utils/request'

export function listPurchaseIn(params) {
  return request({ url: '/v1/purchase-ins', method: 'get', params })
}

export function getPurchaseIn(id) {
  return request({ url: `/v1/purchase-ins/${id}`, method: 'get' })
}

export function addPurchaseIn(data) {
  return request({ url: '/v1/purchase-ins', method: 'post', data })
}

export function updatePurchaseIn(id, data) {
  return request({ url: `/v1/purchase-ins/${id}`, method: 'put', data })
}

export function deletePurchaseIn(id) {
  return request({ url: `/v1/purchase-ins/${id}`, method: 'delete' })
}
