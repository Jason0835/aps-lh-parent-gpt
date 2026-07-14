import request, { downloadLink } from '@/utils/request'

export function listUnscheduleResult(query) {
  return request({ url: '/cd15/cd15UnscheduleResult/list', method: 'post', data: query })
}

export function getUnscheduleResult(id) {
  return request({ url: `/cd15/cd15UnscheduleResult/getInfo/${id}`, method: 'get' })
}

export function exportUnscheduleResult(query) {
  return downloadLink('/cd15/cd15UnscheduleResult/export', query)
}
