import request, { downloadLink } from '@/utils/request'

export function listGdyyScheduleResult(query) {
  return request({
    url: '/gdyy/scheduleResult/list',
    method: 'post',
    data: query
  })
}

export function getGdyyScheduleResult(id) {
  return request({
    url: `/gdyy/scheduleResult/getInfo/${id}`,
    method: 'get'
  })
}

export function addGdyyScheduleResult(data) {
  return request({
    url: '/gdyy/scheduleResult/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function updateGdyyScheduleResult(data) {
  return request({
    url: '/gdyy/scheduleResult/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    },
    data
  })
}

export function delGdyyScheduleResult(data) {
  return request({
    url: '/gdyy/scheduleResult/remove',
    method: 'post',
    data
  })
}

export function exportGdyyScheduleResult(query) {
  return downloadLink('/gdyy/scheduleResult/export', query)
}

export function changeQtyGdyyScheduleResult(data) {
  return request({
    url: '/gdyy/scheduleResult/changeQty',
    method: 'post',
    data
  })
}

export function changeMachineGdyyScheduleResult(data) {
  return request({
    url: '/gdyy/scheduleResult/changeMachine',
    method: 'post',
    data
  })
}

export function publishGdyyScheduleResult(data) {
  return request({
    url: '/gdyy/scheduleResult/publish',
    method: 'post',
    data
  })
}
