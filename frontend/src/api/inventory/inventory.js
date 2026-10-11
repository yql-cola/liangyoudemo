import request from '@/utils/request'

export function listInventory(params) {
  return request({ url: '/v1/inventories', method: 'get', params })
}

export function listInventoryBatches(productId, params) {
  return request({ url: `/v1/inventories/${productId}/batches`, method: 'get', params })
}

export function listInventoryFlows(params) {
  return request({ url: '/v1/inventories/flows', method: 'get', params })
}

export function updateInventory(id, data) {
  return request({ url: `/v1/inventories/${id}`, method: 'put', data })
}

export function deleteInventory(id) {
  return request({ url: `/v1/inventories/${id}`, method: 'delete' })
}
