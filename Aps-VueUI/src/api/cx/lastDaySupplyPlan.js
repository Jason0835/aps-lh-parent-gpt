import request from '@/utils/request'

// 查询基础数据_地区列表
export function listLastDaySupplyPlan(query) {
  return request({
    url: 'cx/lastDaySupplyPlan/list',
    method: 'post',
    data: query
  })
}
export function generateSupplyPlan(query) {
  return request({
    url: 'cx/lastDaySupplyPlan/generateSupplyPlan',
    method: 'post',
    data: query
  })
}
export function regenerateSupplyPlan(query) {
  return request({
    url: 'cx/lastDaySupplyPlan/regenerateSupplyPlan',
    method: 'post',
    data: query
  })
}
export function confirmSupplyPlan(query) {
  return request({
    url: 'cx/lastDaySupplyPlan/confirmSupplyPlan',
    method: 'post',
    data: query
  })
}
export function removeSupplyPlan(query) {
  return request({
    url: 'cx/lastDaySupplyPlan/remove',
    method: 'post',
    data: query
  })
}
export function saveSupplyPlan(query) {
  return request({
    url: 'cx/lastDaySupplyPlan/save',
    method: 'post',
    data: query
  })
}



