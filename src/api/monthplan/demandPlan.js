import request from '@/utils/request'

// =
export function listDemandPlan(query) {
  return request({
    url: '/monthplan/demandPlan/list',
    method: 'post',
    data: query
  })
}
export function saveDemandPlan(query) {
  return request({
    url: '/monthplan/demandPlan/save',
    method: 'post',
    data: query
  })
}
export function genenrDemandPlan(query) {
  return request({
    url: '/monthplan/demandPlan/createMonthRequire',
    method: 'post',
    data: query
  })
}
export function getVersion(query) {
  return request({
    url: '/monthplan/demandPlan/createMonthRequireVersion',
    method: 'post',
    data: query
  })
}

export function getVersionSelect(query) {
  return request({
    url: '/monthplan/demandPlan/findMonthPlanVersion',
    method: 'post',
    data: query
  })
}