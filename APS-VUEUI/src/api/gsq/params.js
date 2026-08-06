import request from '@/utils/request'

// =
export function listParams(query) {
  return request({
    url: '/gsq/params/list',
    method: 'post',
    data: query
  })
}
export function saveParams(data) {
  return request({
    url: '/gsq/params/save',
    method: 'post',
    data: data
  })
}
export function removeParams(query) {
  return request({
    url: '/gsq/params/remove',
    method: 'post',
    data: query
  })
}
export function getInfo(id) {
  return request({
    url: '/gsq/params/' + id,
    method: 'get'
  })
}
