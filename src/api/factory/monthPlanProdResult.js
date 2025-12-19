import request from '@/utils/request'

// =
export function listMonthPlanProdResult(query) {
  return request({
    url: '/factory/monthPlanProdResult/list',
    method: 'post',
    data: query
  })
}
export function editMonthPlanProdResult(query) {
  return request({
    url: '/factory/monthPlanProdResult/save',
    method: 'post',
    data: query
  })
}

export function removeMonthPlanProdResult(query) {
  return request({
    url: '/factory/monthPlanProdResult/remove',
    method: 'post',
    data: query
  })
}

export function statistics(query) {
  return request({
    url: '/factory/monthPlanProdResult/statistics',
    method: 'post',
    data: query
  })
}
export function getProductionMonthType(query) {
  return request({
    url: '/factory/monthPlanProdResult/getProductionMonthType',
    method: 'post',
    data: query,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
  })
}