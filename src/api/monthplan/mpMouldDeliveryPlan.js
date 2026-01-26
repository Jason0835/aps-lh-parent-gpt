import request from '@/utils/request'
export function listMpMouldShellInfo(query) {
  return request({
    url: '/monthplan/mpMouldDeliveryPlan/list',
    method: 'post',
    data: query
  })
}
export function editMpMouldShellInfo(query) {
  return request({
    url: '/monthplan/mpMouldDeliveryPlan/save',
    method: 'post',
    data: query
  })
}
export function removeMpMouldShellInfo(query) {
  return request({
    url: '/monthplan/mpMouldDeliveryPlan/remove',
    method: 'post',
    data: query
  })
}
export function getBoardingDate(query) {
  return request({
    url: '/monthplan/mpMouldDeliveryPlan/getBoardingDate',
    method: 'post',
    data: query
  })
}
export function updateMaterial(query) {
  return request({
    url: '/monthplan/mpMouldDeliveryPlan/updateMainPatternToMaterial',
    method: 'post',
    data: query
  })
}