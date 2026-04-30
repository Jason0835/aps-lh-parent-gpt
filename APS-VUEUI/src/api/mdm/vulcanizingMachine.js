import request, { downloadLink } from '@/utils/request'

// =
export function listVulcanizingMachine(query) {
  return request({
    url: '/mdm/vulcanizingMachine/list',
    method: 'post',
    data: query
  })
}
export function editVulcanizingMachine(query) {
  return request({
    url: '/mdm/vulcanizingMachine/save',
    method: 'post',
    data: query
  })
}
export function removeVulcanizingMachine(query) {
  return request({
    url: '/mdm/vulcanizingMachine/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/mdm/vulcanizingMachine/export', query)
}
