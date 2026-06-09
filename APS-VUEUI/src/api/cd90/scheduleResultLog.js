import request, { downloadLink } from '@/utils/request'

export function listScheduleResultLog(query) {
  return request({ url: '/cd90/cd90ScheduleResultLog/list', method: 'post', data: query })
}
export function getScheduleResultLog(id) {
  return request({ url: `/cd90/cd90ScheduleResultLog/getInfo/${id}`, method: 'get' })
}
export function delScheduleResultLog(data) {
  return request({ url: '/cd90/cd90ScheduleResultLog/remove', method: 'post', data })
}
export function exportScheduleResultLog(query) {
  return downloadLink('/cd90/cd90ScheduleResultLog/export', query)
}
