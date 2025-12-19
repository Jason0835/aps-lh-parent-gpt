import request, { downloadLink } from '@/utils/request'

// =
export function listMdmConstructionInfo(query) {
  return request({
    url: '/monthplan/mdmConstructionInfo/list',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/monthplan/mdmConstructionInfo/export', query)
}
