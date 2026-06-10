import request from '@/utils/request'

export function listTmScheduleResult(query) {
  return request({
    url: '/tm/tmScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function saveTmScheduleResult(data) {
  return request({
    url: '/tm/tmScheduleResult/save',
    method: 'post',
    data: data
  })
}
export function removeTmScheduleResult(query) {
  return request({
    url: '/tm/tmScheduleResult/remove',
    method: 'post',
    data: query
  })
}
export function getTmScheduleResult(id) {
  return request({
    url: '/tm/tmScheduleResult/' + id,
    method: 'get'
  })
}
