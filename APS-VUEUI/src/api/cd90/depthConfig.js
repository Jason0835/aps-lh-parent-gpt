import request, { downloadLink } from '@/utils/request'

export function listDepthConfig(query) {
  return request({ url: '/cd90/cd90DepthConfig/list', method: 'post', data: query })
}
export function getDepthConfig(id) {
  return request({ url: `/cd90/cd90DepthConfig/getInfo/${id}`, method: 'get' })
}
export function addDepthConfig(data) {
  return request({ url: '/cd90/cd90DepthConfig/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateDepthConfig(data) {
  return request({ url: '/cd90/cd90DepthConfig/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delDepthConfig(data) {
  return request({ url: '/cd90/cd90DepthConfig/remove', method: 'post', data })
}
export function exportDepthConfig(query) {
  return downloadLink('/cd90/cd90DepthConfig/export', query)
}
