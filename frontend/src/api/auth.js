import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/v1/auth/login',
    method: 'post',
    data
  })
}

export function getCurrentUser() {
  return request({
    url: '/v1/auth/me',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/v1/auth/logout',
    method: 'post'
  })
}
