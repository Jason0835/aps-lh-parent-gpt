import request, { downloadLink } from '@/utils/request'

// =
export function listMdmInterestRate(query) {
  return request({
    url: '/monthplan/mdmInterestRate/list',
    method: 'post',
    data: query
  })
}
export function editMdmInterestRate(query) {
  return request({
    url: '/monthplan/mdmInterestRate/save',
    method: 'post',
    data: query
  })
}
export function removeMdmInterestRate(query) {
  return request({
    url: '/monthplan/mdmInterestRate/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmInterestRate/export', query)
}
