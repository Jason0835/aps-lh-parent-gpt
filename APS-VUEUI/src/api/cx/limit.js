import request from '@/utils/request'

// =
export function listLimit(query) {
  return request({
    url: '/cx/limit/list',
    method: 'post',
    data: query
  })
}
export function editLimit(query) {
  return request({
    url: '/cx/limit/edit',
    method: 'post',
    data: query
  })
}
export function removeLimit(query) {
  return request({
    url: '/cx/limit/remove',
    method: 'post',
    data: query
  })
}


