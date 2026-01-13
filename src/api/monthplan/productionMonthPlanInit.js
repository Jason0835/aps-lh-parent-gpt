import request, { downloadLink } from '@/utils/request'

// =
export function listProductionMonthPlanInit(query) {
  return request({
    url: '/monthplan/mpStructureAllocation/list',
    method: 'post',
    data: query
  })
}
export function editProductionMonthPlanInit(query) {
  return request({
    url: '/monthplan/productionMonthPlanInit/save',
    method: 'post',
    data: query
  })
}
export function removeProductionMonthPlanInit(query) {
  return request({
    url: '/monthplan/productionMonthPlanInit/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/productionMonthPlanInit/export', query)
}
