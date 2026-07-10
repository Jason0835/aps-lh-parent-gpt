import request, { downloadLink } from '@/utils/request'

export function listDepthConfig(query) {
  return request({ url: '/cd15/cd15DepthConfig/list', method: 'post', data: query })
}

export function getDepthConfig(id) {
  return request({ url: `/cd15/cd15DepthConfig/getInfo/${id}`, method: 'get' })
}

export function addDepthConfig(data) {
  return request({ url: '/cd15/cd15DepthConfig/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}

export function updateDepthConfig(data) {
  return request({ url: '/cd15/cd15DepthConfig/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}

export function delDepthConfig(data) {
  return request({ url: '/cd15/cd15DepthConfig/remove', method: 'post', data })
}

export function exportDepthConfig(query) {
  return downloadLink('/cd15/cd15DepthConfig/export', query)
}

