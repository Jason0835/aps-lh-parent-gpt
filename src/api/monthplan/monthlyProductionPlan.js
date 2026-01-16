import request from '@/utils/request'

export function listProductionPlan(query) {
  return request({
    url: '/monthplan/factoryMonthPlanMouldDayResult/list',
    method: 'post',
    data: query
  })
}


export function listProduction(query) {
  return request({
    url: '/monthplan/factoryMonthPlanFinalResult/list',
    method: 'post',
    data: query
  })
}
export function getProductionMonthType(query) {
  return request({
    url: '/factory/console/getProductionMonthType',
    method: 'post',
    data: query
  })
}
