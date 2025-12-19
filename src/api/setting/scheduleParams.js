import request from '@/utils/request'

// =
export function listScheduleParams(query) {
  return request({
    url: '/setting/scheduleParams/list',
    method: 'post',
    data: query
  })
}
export function removeScheduleParams(query) {
  return request({
    url: '/setting/scheduleParams/remove',
    method: 'post',
    data: query
  })
}
export function saveScheduleParams(query) {
  return request({
    url: '/setting/scheduleParams/edit',
    method: 'post',
    data: query
  })
}
