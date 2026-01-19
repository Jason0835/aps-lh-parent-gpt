import request from '@/utils/request'

export function listVulcanizationTable(query) {
  return request({
    url: '/monthplan/mpMonthPlanMonitor/list',
    method: 'post',
    data: query
  })
}
