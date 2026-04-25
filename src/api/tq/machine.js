import request,{ downloadLink } from '@/utils/request'

export function listMachine(query) {
  return request({
    url: '/tq/machine/list',
    method: 'post',
    data: query
  })
}

export function listEnabledMachines() {
  return request({
    url: '/tq/machine/listEnabledMachines',
    method: 'post'
  })
}
export function addMachine(query) {
  return request({
    url: '/tq/machine/save',
    method: 'post',
    data: query
  })
}
export function editMachine(query) {
  return request({
    url: '/tq/machine/save',
    method: 'post',
    data: query
  })
}
export function removeMachine(query) {
  return request({
    url: '/tq/machine/remove',
    method: 'post',
    data: query
  })
}
export function checkMachineCodeUnique(query) {
  return request({
    url: '/tq/machine/checkUnique',
    method: 'post',
    data: query
  })
}
export function exportData(query) {
  return downloadLink("/tq/machine/export", query);
}
