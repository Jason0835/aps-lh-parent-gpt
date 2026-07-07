import request, { downloadLink } from '@/utils/request'

export function listMachineMaintenancePlan(query) {
  return request({ url: '/cd15/cd15MachineMaintenance/list', method: 'post', data: query })
}
export function getMachineMaintenancePlan(id) {
  return request({ url: `/cd15/cd15MachineMaintenance/getInfo/${id}`, method: 'get' })
}
export function addMachineMaintenancePlan(data) {
  return request({ url: '/cd15/cd15MachineMaintenance/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateMachineMaintenancePlan(data) {
  return request({ url: '/cd15/cd15MachineMaintenance/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delMachineMaintenancePlan(data) {
  return request({ url: '/cd15/cd15MachineMaintenance/remove', method: 'post', data })
}
export function exportMachineMaintenancePlan(query) {
  return downloadLink('/cd15/cd15MachineMaintenance/export', query)
}