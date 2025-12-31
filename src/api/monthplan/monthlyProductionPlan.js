import request from '@/utils/request'

export function listProductionPlan(query) {
  return request({
    url: '/monthplan/factoryMonthPlanMouldDayResult/list',
    method: 'post',
    data: query
  })
}

