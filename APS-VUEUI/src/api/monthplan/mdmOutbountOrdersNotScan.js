import request from '@/utils/request'

export function listMdmOutbountOrdersNotScan(query) {
  return request({
    url: '/monthplan/mdmOutbountOrdersNotScan/list',
    method: 'post',
    data: query
  })
}

export function exportMdmOutbountOrdersNotScan(query) {
  return request({
    url: '/monthplan/mdmOutbountOrdersNotScan/export',
    method: 'post',
    data: query
  })
}
