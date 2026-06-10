import request, { downloadLink } from '@/utils/request'

export function listMachineMaintenancePlan(query) {
  return request({ url: '/cd90/cd90MachineMaintenance/list', method: 'post', data: query })
}
export function getMachineMaintenancePlan(id) {
  return request({ url: `/cd90/cd90MachineMaintenance/getInfo/${id}`, method: 'get' })
}
export function addMachineMaintenancePlan(data) {
  return request({ url: '/cd90/cd90MachineMaintenance/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateMachineMaintenancePlan(data) {
  return request({ url: '/cd90/cd90MachineMaintenance/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delMachineMaintenancePlan(data) {
  return request({ url: '/cd90/cd90MachineMaintenance/remove', method: 'post', data })
}
export function exportMachineMaintenancePlan(query) {
  return downloadLink('/cd90/cd90MachineMaintenance/export', query)
}
