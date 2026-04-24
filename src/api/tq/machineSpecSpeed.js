import request, { downloadLink } from '@/utils/request'

export function listMachineSpecSpeed(query) {
  return request({
    url: '/tq/machineSpecSpeed/list',
    method: 'post',
    data: query
  })
}

export function saveMachineSpecSpeed(query) {
  return request({
    url: '/tq/machineSpecSpeed/save',
    method: 'post',
    data: query
  })
}

export function removeMachineSpecSpeed(ids) {
  return request({
    url: '/tq/machineSpecSpeed/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportMachineSpecSpeed(query) {
  return downloadLink("/tq/machineSpecSpeed/export", query)
}
