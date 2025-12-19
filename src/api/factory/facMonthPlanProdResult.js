import request from '@/utils/request'

// =
export function listFacMonthPlanProdResult(query) {
  return request({
    url: '/factory/facMonthPlanProdResult/list',
    method: 'post',
    data: query
  })
}
export function editFacMonthPlanProdResult(query) {
  return request({
    url: '/factory/facMonthPlanProdResult/save',
    method: 'post',
    data: query
  })
}

export function removeFacMonthPlanProdResult(query) {
  return request({
    url: '/factory/facMonthPlanProdResult/remove',
    method: 'post',
    data: query
  })
}


