import request, { downloadLink } from '@/utils/request'

// =
export function listProductMoldingLimit(query) {
  return request({
    url: '/mdm/productMoldingLimit/list',
    method: 'post',
    data: query
  })
}
export function editProductMoldingLimit(query) {
  return request({
    url: '/mdm/productMoldingLimit/save',
    method: 'post',
    data: query
  })
}
export function removeProductMoldingLimit(query) {
  return request({
    url: '/mdm/productMoldingLimit/remove',
    method: 'post',
    data: query
  })
}
export function checkUnique(query) {
  return request({
    url: '/mdm/productMoldingLimit/checkUnique',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/mdm/productMoldingLimit/export', query)
}
