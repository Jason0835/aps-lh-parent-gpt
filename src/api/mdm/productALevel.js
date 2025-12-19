import request, { downloadLink } from '@/utils/request'

// =
export function listProductALevel(query) {
  return request({
    url: '/mdm/productALevel/list',
    method: 'post',
    data: query
  })
}
export function editProductALevel(query) {
  return request({
    url: '/mdm/productALevel/save',
    method: 'post',
    data: query
  })
}
export function removeProductALevel(query) {
  return request({
    url: '/mdm/productALevel/remove',
    method: 'post',
    data: query
  })
}
export function checkUnique(query) {
  return request({
    url: '/mdm/productALevel/checkUnique',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/mdm/productALevel/export', query)
}
