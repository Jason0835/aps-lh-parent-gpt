import request, { downloadLink } from '@/utils/request'

export function listMachineChuck(query) {
  return request({
    url: '/tq/machineChuck/list',
    method: 'post',
    data: query
  })
}

export function saveMachineChuck(query) {
  return request({
    url: '/tq/machineChuck/save',
    method: 'post',
    data: query
  })
}

export function removeMachineChuck(ids) {
  return request({
    url: '/tq/machineChuck/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function removeAllMachineChuck() {
  return request({
    url: '/tq/machineChuck/removeAll',
    method: 'post'
  })
}

export function exportMachineChuck(query) {
  return downloadLink("/tq/machineChuck/export", query)
}
