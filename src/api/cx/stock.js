import request from '@/utils/request'

//
export function listStock(query) {
  return request({
    url: '/cx/stock/list',
    method: 'post',
    data: query
  })
}
export function editStock(query) {
  return request({
    url: '/cx/stock/edit',
    method: 'post',
    data: query
  })
}
export function removeStock(query) {
  return request({
    url: '/cx/stock/remove',
    method: 'post',
    data: query
  })
}
export function releaseStock(query) {
  return request({
    url: '/cx/stock/releaseStock',
    method: 'post',
    data: query
  })
}
export function getProductEmbryoVersions(query) {
  return request({
    url: '/cx/stock/getProductEmbryoVersions',
    method: 'post',
    data: query
  })
}

