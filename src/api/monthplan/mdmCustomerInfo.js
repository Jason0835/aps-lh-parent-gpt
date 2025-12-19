import request, { downloadLink } from '@/utils/request'

// =
export function listMdmCustomerInfo(query) {
  return request({
    url: '/monthplan/mdmCustomerInfo/list',
    method: 'post',
    data: query
  })
}
export function editMdmCustomerInfo(query) {
  return request({
    url: '/monthplan/mdmCustomerInfo/save',
    method: 'post',
    data: query
  })
}
export function removeMdmCustomerInfo(query) {
  return request({
    url: '/monthplan/mdmCustomerInfo/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmCustomerInfo/export', query)
}
