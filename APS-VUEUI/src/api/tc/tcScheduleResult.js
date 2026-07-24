import request from '@/utils/request'

export function queryScheduleBoard(query) {
  return request({
    url: '/tc/tcScheduleResult/board',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function listUnplanned(query) {
  return request({
    url: '/tc/tcScheduleResult/unplanned/list',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function getResultExplain(resultId) {
  return request({
    url: `/tc/tcScheduleResult/explain/result/${resultId}`,
    method: 'get'
  })
}

export function getUnplannedExplain(unplannedId) {
  return request({
    url: `/tc/tcScheduleResult/explain/unplanned/${unplannedId}`,
    method: 'get'
  })
}

export function getManualOptions(query) {
  return request({
    url: '/tc/tcScheduleResult/manual/options',
    method: 'get',
    params: query
  })
}

export function insertTask(query) {
  return request({
    url: '/tc/tcScheduleResult/operation/insertTask',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function changeQty(query) {
  return request({
    url: '/tc/tcScheduleResult/operation/changeQty',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function changeMachine(query) {
  return request({
    url: '/tc/tcScheduleResult/operation/changeMachine',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function removeScheduleResult(resultIdList) {
  return request({
    url: '/tc/tcScheduleResult/operation/remove',
    method: 'delete',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: resultIdList
  })
}

export function getOperationTask(taskId) {
  return request({
    url: `/tc/tcScheduleResult/operation/task/${taskId}`,
    method: 'get'
  })
}

export function getLatestOperationTask(query) {
  return request({
    url: '/tc/tcScheduleResult/operation/task/latest',
    method: 'get',
    params: query
  })
}

export function validateAutoPlan(query) {
  return request({
    url: '/tc/tcScheduleResult/validateAutoPlan',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function autoPlan(query) {
  return request({
    url: '/tc/tcScheduleResult/autoPlan',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function getAutoPlanTask(taskId) {
  return request({
    url: `/tc/tcScheduleResult/autoPlan/task/${taskId}`,
    method: 'get'
  })
}

export function getLatestAutoPlanTask(query) {
  return request({
    url: '/tc/tcScheduleResult/autoPlan/task/latest',
    method: 'get',
    params: query
  })
}

export function clearAutoPlanRedisCache(query) {
  return request({
    url: '/tc/tcScheduleResult/clearAutoPlanRedisCache',
    method: 'post',
    params: query
  })
}

export function validateRelease(query) {
  return request({
    url: '/tc/tcScheduleResult/release/validate',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function releaseScheduleResult(query) {
  return request({
    url: '/tc/tcScheduleResult/release',
    method: 'post',
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    data: query
  })
}

export function getReleaseTask(taskId) {
  return request({
    url: `/tc/tcScheduleResult/release/task/${taskId}`,
    method: 'get'
  })
}

export function getLatestReleaseTask(query) {
  return request({
    url: '/tc/tcScheduleResult/release/task/latest',
    method: 'get',
    params: query
  })
}
