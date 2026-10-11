import request from '@/utils/request'

export function listUser(params) {
  return request({ url: '/v1/users', method: 'get', params })
}

export function getUser(id) {
  return request({ url: `/v1/users/${id}`, method: 'get' })
}

export function addUser(data) {
  return request({ url: '/v1/users', method: 'post', data })
}

export function updateUser(id, data) {
  return request({ url: `/v1/users/${id}`, method: 'put', data })
}

export function deleteUser(id) {
  return request({ url: `/v1/users/${id}`, method: 'delete' })
}

export function assignUserRoles(id, data) {
  return request({ url: `/v1/users/${id}/roles`, method: 'post', data })
}

export function assignUserPermissions(id, data) {
  return request({ url: `/v1/users/${id}/permissions`, method: 'post', data })
}

export function getUserPermissions(id) {
  return request({ url: `/v1/users/${id}/permissions`, method: 'get' })
}
