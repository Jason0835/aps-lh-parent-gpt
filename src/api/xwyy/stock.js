import request from '@/utils/request'

//
export function listStock(query) {
  return request({
    url: '/xwyy/stock/list',
    method: 'post',
    data: query
  })
}
export function editStock(query) {
  return request({
    url: '/xwyy/stock/edit',
    method: 'post',
    data: query
  })
}
export function removeStock(query) {
  return request({
    url: '/xwyy/stock/remove',
    method: 'post',
    data: query
  })
}
export function releaseStock(query) {
  return request({
    url: '/xwyy/stock/releaseStock',
    method: 'post',
    data: query
  })
}


