import request from '@/utils/request'

// =
export function listMpTrialPlan(query) {
  return request({
    url: '/monthplan/mpTrialPlan/list',
    method: 'post',
    data: query
  })
}
export function saveMpTrialPlan(query) {
  return request({
    url: '/monthplan/mpTrialPlan/save',
    method: 'post',
    data: query
  })
}
export function removeMpTrialPlan(query) {
  return request({
    url: '/monthplan/mpTrialPlan/remove',
    method: 'post',
    data: query
  })
}
