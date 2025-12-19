import request, { downloadLink } from '@/utils/request'

// =
export function listMdmProSizeWeight(query) {
  return request({
    url: '/monthplan/mdmProSizeWeight/list',
    method: 'post',
    data: query
  })
}
export function editMdmProSizeWeight(query) {
  return request({
    url: '/monthplan/mdmProSizeWeight/save',
    method: 'post',
    data: query
  })
}
export function removeMdmProSizeWeight(query) {
  return request({
    url: '/monthplan/mdmProSizeWeight/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmProSizeWeight/export', query)
}
