import request from '@/utils/request'

//
export function listStock(query) {
  return request({
    url: '/dj/stock/list',
    method: 'post',
    data: query
  })
}
export function editStock(query) {
  return request({
    url: '/dj/stock/save',
    method: 'post',
    data: query
  })
}
export function removeStock(query) {
  return request({
    url: '/dj/stock/remove',
    method: 'post',
    data: query
  })
}
export function releaseStock(query) {
  return request({
    url: '/dj/stock/releaseStock',
    method: 'post',
    data: query
  })
}


