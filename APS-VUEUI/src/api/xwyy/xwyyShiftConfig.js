import request, { downloadLink } from '@/utils/request'

export function listShiftConfig(query) {
  return request({ url: '/xwyy/xwyyShiftConfig/list', method: 'post', data: query })
}
export function getShiftConfig(id) {
  return request({ url: `/xwyy/xwyyShiftConfig/getInfo/${id}`, method: 'get' })
}
export function addShiftConfig(data) {
  return request({ url: '/xwyy/xwyyShiftConfig/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateShiftConfig(data) {
  return request({ url: '/xwyy/xwyyShiftConfig/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delShiftConfig(data) {
  return request({ url: '/xwyy/xwyyShiftConfig/remove', method: 'post', data })
}
export function changeStatus(data) {
  return request({ url: '/xwyy/xwyyShiftConfig/changeStatus', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function exportShiftConfig(query) {
  return downloadLink('/xwyy/xwyyShiftConfig/export', query)
}
