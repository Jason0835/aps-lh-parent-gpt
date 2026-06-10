import request from '@/utils/request'

export function listTmStock(query) {
  return request({
    url: '/tm/tmStock/list',
    method: 'post',
    data: query
  })
}
export function saveTmStock(data) {
  return request({
    url: '/tm/tmStock/save',
    method: 'post',
    data: data
  })
}
export function removeTmStock(query) {
  return request({
    url: '/tm/tmStock/remove',
    method: 'post',
    data: query
  })
}
export function getTmStock(id) {
  return request({
    url: '/tm/tmStock/' + id,
    method: 'get'
  })
}
