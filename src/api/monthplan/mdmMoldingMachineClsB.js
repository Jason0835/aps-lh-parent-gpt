import request, { downloadLink } from '@/utils/request'

// =
export function listMdmMoldingMachineClsB(query) {
  return request({
    url: '/monthplan/mdmMoldingMachineClsB/list',
    method: 'post',
    data: query
  })
}
export function editMdmMoldingMachineClsB(query) {
  return request({
    url: '/monthplan/mdmMoldingMachineClsB/save',
    method: 'post',
    data: query
  })
}
export function removeMdmMoldingMachineClsB(query) {
  return request({
    url: '/monthplan/mdmMoldingMachineClsB/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmMoldingMachineClsB/export', query)
}
