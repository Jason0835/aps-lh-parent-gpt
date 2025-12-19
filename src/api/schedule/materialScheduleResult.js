import request from '@/utils/request'

// =
export function listScheduleResult(query) {
  return request({
    url: '/schedule/materialScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function editGlueScheduleResult(query) {
  return request({
    url: '/schedule/materialScheduleResult/edit',
    method: 'post',
    data: query
  })
}

export function removeGlueScheduleResult(query) {
  return request({
    url: '/schedule/materialScheduleResult/remove',
    method: 'post',
    data: query
  })
}
export function validateAutoPlan(query) {
  return request({
    url: '/schedule/materialScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function checkScheduleDateAndMixAreaExist(query) {
  return request({
    url: '/schedule/materialScheduleResult/checkScheduleDateAndMixAreaExist',
    method: 'post',
    data: query
  })
}

export function autoSchedule(query) {
  return request({
    url: '/schedule/materialScheduleResult/autoSchedule',
    method: 'post',
    data: query
  })
}


export function statistics(query) {
  return request({
    url: '/schedule/materialScheduleResult/statistics',
    method: 'post',
    data: query
  })
}
export function expireWarning(query) {
  return request({
    url: '/schedule/materialScheduleResult/expireWarning',
    method: 'post',
    data: query
  })
}


