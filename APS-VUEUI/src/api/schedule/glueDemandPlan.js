import request from '@/utils/request'

// =
export function listGlueDemandPlan(query) {
  return request({
    url: '/schedule/glueDemandPlan/list',
    method: 'post',
    data: query
  })
}
export function editGlueDemandPlan(query) {
  return request({
    url: '/schedule/glueDemandPlan/save',
    method: 'post',
    data: query
  })
}

export function removeGlueDemandPlan(query) {
  return request({
    url: '/schedule/glueDemandPlan/remove',
    method: 'post',
    data: query
  })
}
export function rematchGlueDemandPlan(query) {
  return request({
    url: '/schedule/glueDemandPlan/rematch',
    method: 'post',
    data: query
  })
}

/**
 * 抓取数据
 * @param {*} query
 * @returns
 */
export function grabGlueDemandPlan(query) {
  return request({
    url: '/schedule/glueDemandPlan/grab',
    method: 'post',
    data: query
  })
}

