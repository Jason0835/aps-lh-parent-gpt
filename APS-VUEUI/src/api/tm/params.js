import request from '@/utils/request'

// =
export function listParams(query) {
  return request({
    url: '/tm/tmParams/list',
    method: 'post',
    data: query
  })
}
export function saveParams(data) {
  return request({
    url: '/tm/tmParams/save',
    method: 'post',
    data: data
  })
}
export function removeParams(query) {
  return request({
    url: '/tm/tmParams/remove',
    method: 'post',
    data: query
  })
}
export function getInfo(id) {
  return request({
    url: '/tm/tmParams/' + id,
    method: 'get'
  })
}


