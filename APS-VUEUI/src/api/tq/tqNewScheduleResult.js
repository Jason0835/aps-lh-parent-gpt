import request, { downloadLink } from '@/utils/request'

export function listNewScheduleResult(query) {
  return request({
    url: '/tq/newScheduleResult/list',
    method: 'post',
    data: query
  })
}

export function saveNewScheduleResult(query) {
  return request({
    url: '/tq/newScheduleResult/save',
    method: 'post',
    data: query
  })
}

export function removeNewScheduleResult(ids) {
  return request({
    url: '/tq/newScheduleResult/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportNewScheduleResult(query) {
  return downloadLink("/tq/newScheduleResult/export", query)
}

export function importNewScheduleResult(data) {
  return request({
    url: '/tq/newScheduleResult/importData',
    method: 'post',
    data: data
  })
}

export function listScheduleShiftDates(query) {
  return request({
    url: '/tq/newScheduleResult/listScheduleShiftDates',
    method: 'post',
    data: query
  })
}
