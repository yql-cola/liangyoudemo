import request from '@/utils/request'

export function createBasicApi(resource) {
  const url = `/v1/basic/${resource}`
  return {
    list: params => request({ url, method: 'get', params }),
    get: id => request({ url: `${url}/${id}`, method: 'get' }),
    create: data => request({ url, method: 'post', data }),
    update: (id, data) => request({ url: `${url}/${id}`, method: 'put', data }),
    remove: id => request({ url: `${url}/${id}`, method: 'delete' }),
    status: (id, status) => request({ url: `${url}/${id}/status`, method: 'patch', data: { status } }),
    options: params => request({ url: `${url}/options`, method: 'get', params }),
    export: params => request({ url: `${url}/export`, method: 'get', params, responseType: 'blob' }),
    exportSelected: ids => request({ url: `${url}/export`, method: 'post', data: { ids }, responseType: 'blob' }),
    importTemplate: () => request({ url: `${url}/import/template`, method: 'get', responseType: 'blob' }),
    importFile: file => {
      const data = new FormData()
      data.append('file', file)
      return request({ url: `${url}/import`, method: 'post', data })
    }
  }
}

export const getBasicOptions = (resource, params) => createBasicApi(resource).options(params)
export const getCategoryTree = params => request({ url: '/v1/basic/categories/tree', method: 'get', params })
export const getProductUnits = (productId, params) => request({ url: `/v1/basic/products/${productId}/units`, method: 'get', params })
