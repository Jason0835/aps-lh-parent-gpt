import request, { downloadLink } from '@/utils/request'

export function listUnscheduleResult(query) {
  return request({ url: '/cd90/cd90UnscheduleResult/list', method: 'post', data: query })
}
export function getUnscheduleResult(id) {
  return request({ url: `/cd90/cd90UnscheduleResult/getInfo/${id}`, method: 'get' })
}
export function exportUnscheduleResult(query) {
  return downloadLink('/cd90/cd90UnscheduleResult/export', query)
}
