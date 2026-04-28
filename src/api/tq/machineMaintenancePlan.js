import request, { downloadLink } from '@/utils/request'

export function listMachineMaintenancePlan(query) {
  return request({
    url: '/tq/machineMaintenancePlan/list',
    method: 'post',
    data: query
  })
}

export function saveMachineMaintenancePlan(query) {
  return request({
    url: '/tq/machineMaintenancePlan/save',
    method: 'post',
    data: query
  })
}

export function removeMachineMaintenancePlan(ids) {
  return request({
    url: '/tq/machineMaintenancePlan/remove',
    method: 'post',
    params: { ids: ids }
  })
}

export function exportMachineMaintenancePlan(query) {
  return downloadLink("/tq/machineMaintenancePlan/export", query)
}
