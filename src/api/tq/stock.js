import request from '@/utils/request'

//
export function listStock(query) {
  return request({
    url: '/tq/stock/list',
    method: 'post',
    data: query
  })
}
export function editStock(query) {
  return request({
    url: '/tq/stock/edit',
    method: 'post',
    data: query
  })
}
export function removeStock(query) {
  return request({
    url: '/tq/stock/remove',
    method: 'post',
    data: query
  })
}
export function releaseStock(query) {
  return request({
    url: '/tq/stock/releaseStock',
    method: 'post',
    data: query
  })
}


