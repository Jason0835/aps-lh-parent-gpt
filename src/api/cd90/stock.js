import request from '@/utils/request'

//
export function listStock(query) {
  return request({
    url: '/cd90/stock/list',
    method: 'post',
    data: query
  })
}
export function editStock(query) {
  return request({
    url: '/cd90/stock/edit',
    method: 'post',
    data: query
  })
}
export function removeStock(query) {
  return request({
    url: '/cd90/stock/remove',
    method: 'post',
    data: query
  })
}
export function releaseStock(query) {
  return request({
    url: '/cd90/stock/releaseStock',
    method: 'post',
    data: query
  })
}


