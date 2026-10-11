import request from '@/utils/request'

export function getMenuTree() {
  return request({ url: '/v1/menus/tree', method: 'get' })
}
