import request, { downloadLink } from '@/utils/request'

export function listScheduleResult(query) {
  return request({ url: '/cd90/cd90ScheduleResult/list', method: 'post', data: query })
}
export function getScheduleResult(id) {
  return request({ url: `/cd90/cd90ScheduleResult/getInfo/${id}`, method: 'get' })
}
export function delScheduleResult(data) {
  return request({ url: '/cd90/cd90ScheduleResult/remove', method: 'post', data })
}
export function autoScheduleResult(data) {
  return request({ url: '/cd90/cd90ScheduleResult/autoSchedule', method: 'post', data })
}
export function getAutoScheduleTask(taskId) {
  return request({ url: `/cd90/cd90ScheduleResult/autoSchedule/task/${taskId}`, method: 'get' })
}
export function shiftDates(data) {
  return request({
    url: '/cd90/cd90ScheduleResult/shiftDates',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}
export function validateInsert(data) {
  return request({
    url: '/cd90/cd90ScheduleResult/validateInsert',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}
export function insertOrder(data) {
  return request({
    url: '/cd90/cd90ScheduleResult/insert',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}
export function getInsertTask(taskId) {
  return request({ url: `/cd90/cd90ScheduleResult/insert/task/${taskId}`, method: 'get' })
}
export function publishScheduleResult(data) {
  return request({ url: '/cd90/cd90ScheduleResult/publish', method: 'post', data })
}
export function exportScheduleResult(query) {
  return downloadLink('/cd90/cd90ScheduleResult/export', query)
}
