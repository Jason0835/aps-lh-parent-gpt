import request, { downloadLink } from '@/utils/request'

export function listProductinfo(query) {
  return request({
    url: '/lean/productinfo/list',
    method: 'post',
    data: query
  })
}
export function editProductinfo(query) {
  return request({
    url: '/lean/productinfo/edit',
    method: 'post',
    data: query
  })
}
export function removeProductinfo(query) {
  return request({
    url: '/lean/productinfo/remove',
    method: 'post',
    data: query
  })
}
export function checkMouldUseStatusUnique(query) {
  return request({
    url: '/lean/productinfo/checkMouldUseStatusUnique',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/lean/productinfo/export', query)
}
