import request from '@/utils/request'

// =
export function listLhflScheduleParams(query) {
  return request({
    url: '/setting/lhflScheduleParams/list',
    method: 'post',
    data: query
  })
}
export function removeLhflScheduleParams(query) {
  return request({
    url: '/setting/lhflScheduleParams/remove',
    method: 'post',
    data: query
  })
}
export function saveLhflScheduleParams(query) {
  return request({
    url: '/setting/lhflScheduleParams/save',
    method: 'post',
    data: query
  })
}
