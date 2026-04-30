import request, { downloadLink } from '@/utils/request'

export function listMouldusestatus(query) {
  return request({
    url: '/lean/mouldusestatus/list',
    method: 'post',
    data: query
  })
}
export function editMouldusestatus(query) {
  return request({
    url: '/lean/mouldusestatus/edit',
    method: 'post',
    data: query
  })
}
export function removeMouldusestatus(query) {
  return request({
    url: '/lean/mouldusestatus/remove',
    method: 'post',
    data: query
  })
}
export function checkMouldUseStatusUnique(query) {
  return request({
    url: '/lean/mouldusestatus/checkMouldUseStatusUnique',
    method: 'post',
    data: query
  })
}
export function mergeMouldUseStatus(query) {
  return request({
    url: '/lean/mouldusestatus/merge',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  downloadLink('/lean/mouldusestatus/export', query)
}
