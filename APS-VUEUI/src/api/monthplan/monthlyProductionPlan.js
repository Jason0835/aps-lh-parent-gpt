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

/** 月计划调整查询页列表（list4Adjust，可传 scheduledMachines 按当前调整机台过滤） */
export function listMonthPlanFinal4Adjust(query) {
  return request({
    url: '/monthplan/factoryMonthPlanFinalResult/list4Adjust',
    method: 'post',
    data: query
  })
}

export function syncAdjustedMonthPlanToScmAndMes(query) {
  return request({
    url: '/monthplan/factoryMonthPlanFinalResult/syncAdjustedMonthPlanToScmAndMes',
    method: 'post',
    data: query
  })
}

export function getFinalResultVersionList(query) {
  return request({
    url: '/monthplan/factoryMonthPlanFinalResult/getVersionList',
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
