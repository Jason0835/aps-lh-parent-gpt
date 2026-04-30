import request, { downloadLink } from '@/utils/request'

// =
export function listMachine(query) {
  return request({
    url: '/setting/machine/list',
    method: 'post',
    data: query
  })
}
export function removeMachine(query) {
  return request({
    url: '/setting/machine/remove',
    method: 'post',
    data: query
  })
}
export function saveMachine(query) {
  return request({
    url: '/setting/machine/save',
    method: 'post',
    data: query
  })
}
export function checkGlueMachineUnique(query) {
  return request({
    url: '/setting/machine/checkGlueMachineUnique',
    method: 'post',
    data: query
  })
}
export function checkComplete(query) {
  return request({
    url: '/setting/machine/checkComplete',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  return downloadLink('/setting/machine/export', query);
}
