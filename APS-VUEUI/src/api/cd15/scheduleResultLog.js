import request, { downloadLink } from '@/utils/request'

export function listScheduleResultLog(query) {
  return request({ url: '/cd15/cd15ScheduleResultLog/list', method: 'post', data: query })
}

export function getScheduleResultLog(id) {
  return request({ url: `/cd15/cd15ScheduleResultLog/getInfo/${id}`, method: 'get' })
}

export function removeScheduleResultLog(ids) {
  return request({ url: '/cd15/cd15ScheduleResultLog/remove', method: 'post', data: ids })
}

export function exportScheduleResultLog(query) {
  return downloadLink('/cd15/cd15ScheduleResultLog/export', query)
}
