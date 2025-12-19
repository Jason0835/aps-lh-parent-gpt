import request, { downloadLink } from '@/utils/request'

// =
export function listSaleOrderAllocation(query) {
  return request({
    url: '/monthplan/SaleOrderAllocation/list',
    method: 'post',
    data: query
  })
}
export function editSaleOrderAllocation(query) {
  return request({
    url: '/monthplan/SaleOrderAllocation/save',
    method: 'post',
    data: query
  })
}
export function removeSaleOrderAllocation(query) {
  return request({
    url: '/monthplan/SaleOrderAllocation/remove',
    method: 'post',
    data: query
  })
}
export function getVersionList(query) {
  return request({
    url: '/monthplan/SaleOrderAllocation/versionList',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/SaleOrderAllocation/export', query)
}
