import request, { downloadLink } from '@/utils/request'

// =
export function listMdmStockUpPlan(query) {
  return request({
    url: '/monthplan/mdmStockUpPlan/list',
    method: 'post',
    data: query
  })
}
export function createStockUpPlan(query) {
  return request({
    url: '/monthplan/mdmStockUpPlan/createStockUpPlan',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmStockUpPlan/export', query)
}
