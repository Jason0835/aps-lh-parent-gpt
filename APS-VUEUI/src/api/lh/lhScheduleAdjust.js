import request,{ downloadLink } from '@/utils/request'


export function listLhScheduleAdjust(query) {
  return request({
    url: '/lh/lhScheduleAdjust/list',
    method: 'post',
    data: query
  })
}
export function removeLhScheduleAdjust(query) {
  return request({
    url: '/lh/lhScheduleAdjust/remove',
    method: 'post',
    data: query
  })
}
export function editLhScheduleAdjust(query) {
  return request({
    url: '/lh/lhScheduleAdjust/save',
    method: 'post',
    data: query
  })
}
export function confirmAdjust(query) {
  return request({
    url: '/lh/lhScheduleAdjust/confirmAdjust',
    method: 'post',
    data: query
  })
}
