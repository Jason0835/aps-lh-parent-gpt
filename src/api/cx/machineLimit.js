import request from '@/utils/request'

// =
export function listMachineLimit(query) {
  return request({
    url: '/cx/machineLimit/list',
    method: 'post',
    data: query
  })
}
export function editMachineLimit(query) {
  return request({
    url: '/cx/machineLimit/edit',
    method: 'post',
    data: query
  })
}
export function removeMachineLimit(query) {
  return request({
    url: '/cx/machineLimit/remove',
    method: 'post',
    data: query
  })
}


