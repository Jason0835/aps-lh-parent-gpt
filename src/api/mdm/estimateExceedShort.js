import request, { downloadLink } from '@/utils/request'

export function listEstimateExceedShort(query) {
  return request({
    url: '/lean/estimateExceedShort/list',
    method: 'post',
    data: query
  })
}
export function editEstimateExceedShort(query) {
  return request({
    url: '/lean/estimateExceedShort/edit',
    method: 'post',
    data: query
  })
}
export function removeEstimateExceedShort(query) {
  return request({
    url: '/lean/estimateExceedShort/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/lean/estimateExceedShort/export', query)
}
