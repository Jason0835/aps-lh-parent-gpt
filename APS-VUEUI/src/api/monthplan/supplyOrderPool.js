import request from '@/utils/request'

export function listSupplyOrderPool(query) {
  return request({
    url: '/monthplan/supplyOrderPool/list',
    method: 'post',
    data: query
  })
}
export function saveSupplyOrderPool(query) {
  return request({
    url: '/monthplan/supplyOrderPool/save',
    method: 'post',
    data: query
  })
}
export function checkSupplyOrderPool(query) {
  return request({
    url: '/monthplan/supplyOrderPool/checkOverdue',
    method: 'post',
    data: query
  })
}
export function removeSupplyOrderPool(query) {
  return request({
    url: '/monthplan/supplyOrderPool/remove',
    method: 'post',
    data: query
  })
}

export function createCycleStockUp(query) {
  return request({
    url: '/monthplan/supplyOrderPool/createCycleStockUp',
    method: 'post',
    data: query
  })
}

export function createPrecedentStockUp(query) {
  return request({
    url: '/monthplan/supplyOrderPool/createPrecedentStockUp',
    method: 'post',
    data: query
  })
}
export function queryRelationByMaterialCode(query) {
  return request({
    url: '/monthplan/supplyOrderPool/queryRelationByMaterialCode',
    method: 'post',
    data: query
  })
}


export function cyclicSchedulingTips(query) {
  return request({
    url: '/monthplan/supplyOrderPool/queryStockUpByMaterialCode',
    method: 'post',
    data: query
  })
}

export function schedulingPate(query) {
  return request({
    url: '/monthplan/supplyOrderPool/setSchedule',
    method: 'post',
    data: query
  })
}
