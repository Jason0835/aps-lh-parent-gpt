import request, { downloadLink } from '@/utils/request'

// =
export function listUnclaimed(query) {
  return request({
    url: '/setting/unclaimed/list',
    method: 'post',
    data: query
  })
}
export function removeUnclaimed(query) {
  return request({
    url: '/setting/unclaimed/remove',
    method: 'post',
    data: query
  })
}
export function saveUnclaimed(query) {
  return request({
    url: '/setting/unclaimed/save',
    method: 'post',
    data: query
  })
}

export function exportData(query) {
  return downloadLink('/setting/unclaimed/export', query);
}
