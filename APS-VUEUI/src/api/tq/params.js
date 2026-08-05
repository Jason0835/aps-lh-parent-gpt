import request from '@/utils/request'

// =
export function listParams(query) {
  return request({
    url: '/tq/params/list',
    method: 'post',
    data: query
  })
}
export function saveParams(data) {
  return request({
    url: '/tq/params/save',
    method: 'post',
    data: data
  })
}
export function removeParams(query) {
  return request({
    url: '/tq/params/remove',
    method: 'post',
    data: query
  })
}
export function getInfo(id) {
  return request({
    url: '/tq/params/' + id,
    method: 'get'
  })
}
