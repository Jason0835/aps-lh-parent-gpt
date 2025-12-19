import request from '@/utils/request'

//
export function listSpecColor(query) {
  return request({
    url: '/cx/specColor/list',
    method: 'post',
    data: query
  })
}
export function editSpecColor(query) {
  return request({
    url: '/cx/specColor/edit',
    method: 'post',
    data: query
  })
}
export function removeSpecColor(query) {
  return request({
    url: '/cx/specColor/remove',
    method: 'post',
    data: query
  })
}


