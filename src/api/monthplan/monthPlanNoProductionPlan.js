import request, { downloadLink } from '@/utils/request'

// =
export function listMonthPlanNoProductionPlan(query) {
  return request({
    url: '/monthplan/monthPlanNoProductionPlan/list',
    method: 'post',
    data: query
  })
}
export function editMonthPlanNoProductionPlan(query) {
  return request({
    url: '/monthplan/monthPlanNoProductionPlan/save',
    method: 'post',
    data: query
  })
}
export function removeMonthPlanNoProductionPlan(query) {
  return request({
    url: '/monthplan/monthPlanNoProductionPlan/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/monthPlanNoProductionPlan/export', query)
}
