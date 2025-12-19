import request from '@/utils/request'

//
export function listStock(query) {
  return request({
    url: '/gsq/stock/list',
    method: 'post',
    data: query
  })
}
export function editStock(query) {
  return request({
    url: '/gsq/stock/edit',
    method: 'post',
    data: query
  })
}
export function removeStock(query) {
  return request({
    url: '/gsq/stock/remove',
    method: 'post',
    data: query
  })
}
export function releaseStock(query) {
  return request({
    url: '/gsq/stock/releaseStock',
    method: 'post',
    data: query
  })
}


