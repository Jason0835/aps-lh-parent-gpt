import request, { downloadLink } from '@/utils/request'

export function listScheduleResult(query) {
  return request({ url: '/cd15/cd15ScheduleResult/list', method: 'post', data: query })
}

export function getScheduleResult(id) {
  return request({ url: `/cd15/cd15ScheduleResult/getInfo/${id}`, method: 'get' })
}

export function delScheduleResult(data) {
  return request({ url: '/cd15/cd15ScheduleResult/remove', method: 'post', data })
}

export function autoSchedule(data) {
  return request({ url: '/cd15/cd15ScheduleResult/autoSchedule', method: 'post', data })
}

export function getAutoScheduleTask(taskId) {
  return request({ url: `/cd15/cd15ScheduleResult/autoSchedule/task/${taskId}`, method: 'get' })
}

export function validateInsert(data) {
  return request({
    url: '/cd15/cd15ScheduleResult/validateInsert',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

export function insert(data) {
  return request({
    url: '/cd15/cd15ScheduleResult/insert',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

export function getInsertTask(taskId) {
  return request({ url: `/cd15/cd15ScheduleResult/insert/task/${taskId}`, method: 'get' })
}

export function validateTransferMachine(data) {
  return request({
    url: '/cd15/cd15ScheduleResult/validateTransferMachine',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

export function transferMachine(data) {
  return request({
    url: '/cd15/cd15ScheduleResult/transferMachine',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

export function getTransferMachineTask(taskId) {
  return request({ url: `/cd15/cd15ScheduleResult/transferMachine/task/${taskId}`, method: 'get' })
}

export function validateChangeQty(data) {
  return request({
    url: '/cd15/cd15ScheduleResult/validateChangeQty',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

export function changeQty(data) {
  return request({
    url: '/cd15/cd15ScheduleResult/changeQty',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data
  })
}

export function getChangeQtyTask(taskId) {
  return request({ url: `/cd15/cd15ScheduleResult/changeQty/task/${taskId}`, method: 'get' })
}

export function publishScheduleResult(data) {
  return request({ url: '/cd15/cd15ScheduleResult/publish', method: 'post', data })
}

export function exportScheduleResult(query) {
  return downloadLink('/cd15/cd15ScheduleResult/export', query)
}