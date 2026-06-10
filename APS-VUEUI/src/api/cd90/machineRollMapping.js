import request, { downloadLink } from '@/utils/request'

export function listMachineRollMapping(query) {
  return request({ url: '/cd90/cd90MachineRollMapping/list', method: 'post', data: query })
}
export function getMachineRollMapping(id) {
  return request({ url: `/cd90/cd90MachineRollMapping/getInfo/${id}`, method: 'get' })
}
export function addMachineRollMapping(data) {
  return request({ url: '/cd90/cd90MachineRollMapping/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function updateMachineRollMapping(data) {
  return request({ url: '/cd90/cd90MachineRollMapping/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}
export function delMachineRollMapping(data) {
  return request({ url: '/cd90/cd90MachineRollMapping/remove', method: 'post', data })
}
export function importMachineRollMapping(data) {
  return request({ url: '/cd90/cd90MachineRollMapping/importData', method: 'post', data })
}
export function exportMachineRollMapping(query) {
  return downloadLink('/cd90/cd90MachineRollMapping/export', query)
}
