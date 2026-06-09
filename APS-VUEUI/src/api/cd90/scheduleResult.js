import request, { downloadLink } from '@/utils/request'

export function listScheduleResult(query) {
  return request({ url: '/cd90/cd90ScheduleResult/list', method: 'post', data: query })
}
export function getScheduleResult(id) {
  return request({ url: `/cd90/cd90ScheduleResult/getInfo/${id}`, method: 'get' })
}
export function delScheduleResult(data) {
  return request({ url: '/cd90/cd90ScheduleResult/remove', method: 'post', data })
}
export function exportScheduleResult(query) {
  return downloadLink('/cd90/cd90ScheduleResult/export', query)
}
