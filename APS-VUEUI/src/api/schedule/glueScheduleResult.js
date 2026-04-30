import request from '@/utils/request'

// =
export function listGlueScheduleResult(query) {
  return request({
    url: '/schedule/glueScheduleResult/list',
    method: 'post',
    data: query
  })
}
export function editGlueScheduleResult(query) {
  return request({
    url: '/schedule/glueScheduleResult/save',
    method: 'post',
    data: query
  })
}

export function removeGlueScheduleResult(query) {
  return request({
    url: '/schedule/glueScheduleResult/remove',
    method: 'post',
    data: query
  })
}
export function validateAutoPlan(query) {
  return request({
    url: '/schedule/glueScheduleResult/validateAutoPlan',
    method: 'post',
    data: query
  })
}
export function checkScheduleDateAndMixAreaExist(query) {
  return request({
    url: '/schedule/glueScheduleResult/checkScheduleDateAndMixAreaExist',
    method: 'post',
    data: query
  })
}

export function autoSchedule(query) {
  return request({
    url: '/schedule/glueScheduleResult/autoSchedule',
    method: 'post',
    data: query
  })
}


export function statistics(query) {
  return request({
    url: '/schedule/glueScheduleResult/statistics',
    method: 'post',
    data: query
  })
}
export function publishGlueScheduleResult(query) {
  return request({
    url: '/schedule/glueScheduleResult/publish',
    method: 'post',
    data: query
  })
}
export function publishValidateGlueScheduleResult(query) {
  return request({
    url: '/schedule/glueScheduleResult/publishValidate',
    method: 'post',
    data: query
  })
}

export function changeMachine(query) {
  return request({
    url: '/schedule/glueScheduleResult/changeMachine',
    method: 'post',
    data: query
  })
}


