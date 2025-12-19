import request from '@/utils/request'

//
export function listStock(query) {
  return request({
    url: '/tc/stock/list',
    method: 'post',
    data: query
  })
}
export function editStock(query) {
  return request({
    url: '/tc/stock/edit',
    method: 'post',
    data: query
  })
}
export function removeStock(query) {
  return request({
    url: '/tc/stock/remove',
    method: 'post',
    data: query
  })
}
export function releaseStock(query) {
  return request({
    url: '/tc/stock/releaseStock',
    method: 'post',
    data: query
  })
}


