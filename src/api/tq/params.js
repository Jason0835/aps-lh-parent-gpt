import request from '@/utils/request'

// =
export function listParams(query) {
  return request({
    url: '/tq/params/list',
    method: 'post',
    data: query
  })
}
export function editParams(query) {
  return request({
    url: '/tq/params/edit',
    method: 'post',
    data: query
  })
}
export function removeParams(query) {
  return request({
    url: '/tq/params/remove',
    method: 'post',
    data: query
  })
}


