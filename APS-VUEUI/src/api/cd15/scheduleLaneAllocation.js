import request, { downloadLink } from '@/utils/request'

export function listScheduleLaneAllocation(query) {
  return request({ url: '/cd15/cd15ScheduleLaneAllocation/list', method: 'post', data: query })
}

export function getScheduleLaneAllocation(id) {
  return request({ url: `/cd15/cd15ScheduleLaneAllocation/getInfo/${id}`, method: 'get' })
}

export function exportScheduleLaneAllocation(query) {
  return downloadLink('/cd15/cd15ScheduleLaneAllocation/export', query)
}
