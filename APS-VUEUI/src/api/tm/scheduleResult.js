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
    url: '/tm/tmScheduleResult/operation/remove',
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

export function validateAutoPlan(query) {
  return request({
    url: '/tm/tmScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}

export function autoPlan(query) {
  return request({
    url: '/tm/tmScheduleResult/autoPlan',
    method: 'post',
    data: query
  })
}

export function getAutoPlanTask(taskId) {
  return request({
    url: '/tm/tmScheduleResult/autoPlan/task/' + taskId,
    method: 'get'
  })
}

export function getLatestAutoPlanTask(query) {
  return request({
    url: '/tm/tmScheduleResult/autoPlan/task/latest',
    method: 'get',
    params: query
  })
}
export function listTmScheduleBoard(query) {
  return request({
    url: '/tm/tmScheduleResult/board',
    method: 'post',
    data: query
  })
}

export function listTmScheduleSummary(query) {
  return request({
    url: '/tm/tmScheduleResult/summary',
    method: 'post',
    data: query
  })
}

export function insertTask(data) {
  return request({
    url: '/tm/tmScheduleResult/operation/insertTask',
    method: 'post',
    data: data
  })
}

export function changeQty(data) {
  return request({
    url: '/tm/tmScheduleResult/operation/changeQty',
    method: 'post',
    data: data
  })
}

export function batchChangeMachine(machineCode, data) {
  return request({
    url: '/tm/tmScheduleResult/operation/batchChangeMachine/' + machineCode,
    method: 'post',
    data: data
  })
}

export function publishValidate(ids) {
  return request({
    url: '/tm/tmScheduleResult/publishValidate',
    method: 'post',
    data: { ids: ids.join(',') }
  })
}

export function publishScheduleResult(ids) {
  return request({
    url: '/tm/tmScheduleResult/operation/publish',
    method: 'post',
    data: { ids: ids.join(',') }
  })
}

export function getOperationTask(taskId) {
  return request({
    url: '/tm/tmScheduleResult/operation/task/' + taskId,
    method: 'get'
  })
}

export function getLatestOperationTask(query) {
  return request({
    url: '/tm/tmScheduleResult/operation/task/latest',
    method: 'get',
    params: query
  })
}

export function listScheduleShiftDates(query) {
  return request({
    url: '/tm/tmScheduleResult/listScheduleShiftDates',
    method: 'post',
    data: query
  })
}

export function changeReleaseStatus(data) {
  return request({
    url: '/tm/tmScheduleResult/changeReleaseStatus',
    method: 'post',
    data: data
  })
}
