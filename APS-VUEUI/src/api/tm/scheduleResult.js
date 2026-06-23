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

export function listTmScheduleBoard(query) {
  return request({
    url: '/tm/tmScheduleResult/board',
    method: 'post',
    data: query
  })
}

export function insertTask(data) {
  return request({
    url: '/tm/tmScheduleResult/insertTask',
    method: 'post',
    data: data
  })
}

export function changeQty(data) {
  return request({
    url: '/tm/tmScheduleResult/changeQty',
    method: 'post',
    data: data
  })
}

export function publishValidate(ids) {
  return request({
    url: '/tm/tmScheduleResult/publishValidate',
    method: 'post',
    data: ids
  })
}

export function publishScheduleResult(ids) {
  return request({
    url: '/tm/tmScheduleResult/publish',
    method: 'post',
    data: ids
  })
}

export function listScheduleShiftDates(query) {
  return request({
    url: '/tm/tmScheduleResult/listScheduleShiftDates',
    method: 'post',
    data: query
  })
}
