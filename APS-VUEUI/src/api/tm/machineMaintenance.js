import request from '@/utils/request'

export function listTmMachineMaintenance(query) {
  return request({
    url: '/tm/tmMachineMaintenance/list',
    method: 'post',
    data: query
  })
}
export function saveTmMachineMaintenance(data) {
  return request({
    url: '/tm/tmMachineMaintenance/save',
    method: 'post',
    data: data
  })
}
export function removeTmMachineMaintenance(query) {
  return request({
    url: '/tm/tmMachineMaintenance/remove',
    method: 'post',
    data: query
  })
}
export function getTmMachineMaintenance(id) {
  return request({
    url: '/tm/tmMachineMaintenance/' + id,
    method: 'get'
  })
}
