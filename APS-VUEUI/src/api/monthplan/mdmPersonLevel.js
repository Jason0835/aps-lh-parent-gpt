import request, { downloadLink } from '@/utils/request'

// =
export function listMdmPersonLevel(query) {
  return request({
    url: '/monthplan/mdmPersonLevel/list',
    method: 'post',
    data: query
  })
}
export function editMdmPersonLevel(query) {
  return request({
    url: '/monthplan/mdmPersonLevel/saveMdmPersonLevel',
    method: 'post',
    data: query
  })
}
export function removeMdmPersonLevel(query) {
  return request({
    url: '/monthplan/mdmPersonLevel/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmPersonLevel/export', query)
}
