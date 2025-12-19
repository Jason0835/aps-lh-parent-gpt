import request, { downloadLink } from '@/utils/request'

// =
export function listMdmDevicePlanShut(query) {
  return request({
    url: '/monthplan/mdmDevicePlanShut/list',
    method: 'post',
    data: query
  })
}
export function editMdmDevicePlanShu(query) {
  return request({
    url: '/monthplan/mdmDevicePlanShut/save',
    method: 'post',
    data: query
  })
}
export function removeMdmDevicePlanShut(query) {
  return request({
    url: '/monthplan/mdmDevicePlanShut/remove',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/maindata/mdmModelInfo/export', query)
}
