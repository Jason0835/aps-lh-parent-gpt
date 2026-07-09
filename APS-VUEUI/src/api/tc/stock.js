import request from '@/utils/request'

export function listTcStock(query) {
  return request({
    url: '/tc/tcStock/list',
    method: 'post',
    data: query
  })
}
export function saveTcStock(data) {
  return request({
    url: '/tc/tcStock/save',
    method: 'post',
    data: data
  })
}
export function removeTcStock(query) {
  return request({
    url: '/tc/tcStock/remove',
    method: 'post',
    data: query
  })
}
export function getTcStock(id) {
  return request({
    url: '/tc/tcStock/' + id,
    method: 'get'
  })
}
