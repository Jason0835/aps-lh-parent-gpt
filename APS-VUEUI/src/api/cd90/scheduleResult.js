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
export function validateTransferMachine(data) {
  return request({
    url: '/cd90/cd90ScheduleResult/validateTransferMachine',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}
export function transferMachine(data) {
  return request({
    url: '/cd90/cd90ScheduleResult/transferMachine',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}
export function getTransferMachineTask(taskId) {
  return request({ url: `/cd90/cd90ScheduleResult/transferMachine/task/${taskId}`, method: 'get' })
}
export function validateChangeQty(data) {
  return request({
    url: '/cd90/cd90ScheduleResult/validateChangeQty',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}
export function changeQty(data) {
  return request({
    url: '/cd90/cd90ScheduleResult/changeQty',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}
export function getChangeQtyTask(taskId) {
  return request({ url: `/cd90/cd90ScheduleResult/changeQty/task/${taskId}`, method: 'get' })
}
export function getTimedRollingTask(taskId) {
  return request({ url: `/cd90/cd90ScheduleResult/rollingSchedule/task/${taskId}`, method: 'get' })
}
export function listTimedRollingAdjustLogs(data) {
  return request({
    url: '/cd90/cd90ScheduleResult/rollingSchedule/adjustLog/list',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}
export function getTimedRollingAdjustLog(id) {
  return request({ url: `/cd90/cd90ScheduleResult/rollingSchedule/adjustLog/${id}`, method: 'get' })
}
export function publishScheduleResult(data) {
  return request({ url: '/cd90/cd90ScheduleResult/publish', method: 'post', data })
}
export function exportScheduleResult(query) {
  return downloadLink('/cd90/cd90ScheduleResult/export', query)
}
