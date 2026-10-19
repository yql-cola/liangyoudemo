import request from '@/utils/request'
export const listInventory = params => request({ url: '/v1/inventories', method: 'get', params })
export const listInventoryBatches = params => request({ url: '/v1/inventories/batches', method: 'get', params })
export const listInventoryFlows = params => request({ url: '/v1/inventories/flows', method: 'get', params })
export const updateInventory = (id, data) => request({ url: `/v1/inventories/${id}`, method: 'put', data })
export const deleteInventory = id => request({ url: `/v1/inventories/${id}`, method: 'delete' })
export const exportInventory = (kind, params) => request({ url: `/v1/inventories${kind === 'stock' ? '' : kind === 'batch' ? '/batches' : '/flows'}/export`, method: 'get', params, responseType: 'blob' })
