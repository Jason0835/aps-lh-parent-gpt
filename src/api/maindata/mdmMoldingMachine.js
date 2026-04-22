import request, { downloadLink } from '@/utils/request'

// =
export function listMdmMoldingMachine(query) {
  return request({
    url: '/monthplan/mdmMoldingMachine/list',
    method: 'post',
    data: query
  })
}
export function editMdmMoldingMachine(query) {
  return request({
    url: '/maindata/mdmMoldingMachine/save',
    method: 'post',
    data: query
  })
}
export function removeMdmMoldingMachine(query) {
  return request({
    url: '/maindata/mdmMoldingMachine/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/maindata/mdmMoldingMachine/export', query)
}
