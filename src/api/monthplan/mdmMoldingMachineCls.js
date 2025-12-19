import request, { downloadLink } from '@/utils/request'

// =
export function listMdmMoldingMachineCls(query) {
  return request({
    url: '/monthplan/mdmMoldingMachineCls/list',
    method: 'post',
    data: query
  })
}
export function editMdmMoldingMachineCls(query) {
  return request({
    url: '/monthplan/mdmMoldingMachineCls/save',
    method: 'post',
    data: query
  })
}
export function removeMdmMoldingMachineCls(query) {
  return request({
    url: '/monthplan/mdmMoldingMachineCls/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmMoldingMachineCls/export', query)
}
