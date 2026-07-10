import request, { downloadLink } from '@/utils/request'

export function listMachineRollMapping(query) {
  return request({ url: '/cd15/machineRollMapping/list', method: 'post', data: query })
}

export function getMachineRollMapping(id) {
  return request({ url: `/cd15/machineRollMapping/getInfo/${id}`, method: 'get' })
}

export function addMachineRollMapping(data) {
  return request({ url: '/cd15/machineRollMapping/add', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}

export function updateMachineRollMapping(data) {
  return request({ url: '/cd15/machineRollMapping/edit', method: 'post', headers: { 'Content-Type': 'application/json;charset=UTF-8' }, data })
}

export function editMachineRollMapping(data) {
  return data && data.id ? updateMachineRollMapping(data) : addMachineRollMapping(data)
}

export function removeMachineRollMapping(data) {
  return request({ url: '/cd15/machineRollMapping/remove', method: 'post', data })
}

export function exportMachineRollMapping(query) {
  return downloadLink('/cd15/machineRollMapping/export', query)
}
