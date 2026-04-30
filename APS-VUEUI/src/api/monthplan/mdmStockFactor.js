import request, { downloadLink } from '@/utils/request'

// =
export function listMdmStockFactor(query) {
  return request({
    url: '/monthplan/mdmStockFactor/list',
    method: 'post',
    data: query
  })
}
export function editMdmStockFactor(query) {
  return request({
    url: '/monthplan/mdmStockFactor/save',
    method: 'post',
    data: query
  })
}
export function removeMdmStockFactor(query) {
  return request({
    url: '/monthplan/mdmStockFactor/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmStockFactor/export', query)
}
