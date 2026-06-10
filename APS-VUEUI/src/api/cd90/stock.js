import request, { downloadLink } from '@/utils/request'

export function listStock(query) {
  return request({ url: '/cd90/cd90Stock/list', method: 'post', data: query })
}
export function getStock(id) {
  return request({ url: `/cd90/cd90Stock/getInfo/${id}`, method: 'get' })
}
export function addStock(data) {
  return request({ url: '/cd90/cd90Stock/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateStock(data) {
  return request({ url: '/cd90/cd90Stock/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delStock(data) {
  return request({ url: '/cd90/cd90Stock/remove', method: 'post', data })
}
export function exportStock(query) {
  return downloadLink('/cd90/cd90Stock/export', query)
}
