import request, { downloadLink } from '@/utils/request'

export function listStock(query) {
  return request({ url: '/xwyy/xwyyStock/list', method: 'post', data: query })
}
export function getStock(id) {
  return request({ url: `/xwyy/xwyyStock/getInfo/${id}`, method: 'get' })
}
export function addStock(data) {
  return request({ url: '/xwyy/xwyyStock/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateStock(data) {
  return request({ url: '/xwyy/xwyyStock/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delStock(data) {
  return request({ url: '/xwyy/xwyyStock/remove', method: 'post', data })
}
export function exportStock(query) {
  return downloadLink('/xwyy/xwyyStock/export', query)
}


